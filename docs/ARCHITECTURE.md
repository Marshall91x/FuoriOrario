# Fuori Orario — Architettura

Decisioni motivate in [adr/](adr/). Termini in [CONTEXT.md](../CONTEXT.md).

## Stack

| Area | Scelta |
|---|---|
| Linguaggio / UI | Kotlin Multiplatform + Compose Multiplatform (versioni del wizard KMP al momento della creazione) |
| Target | Android, iOS (arm64, simulatorArm64), Web `wasmJs` |
| Backend | Supabase (Postgres, Auth, RLS), regione UE |
| Client backend | `supabase-kt` (auth + postgrest), Ktor engine: OkHttp (Android), Darwin (iOS), Js (Wasm) |
| DI | Koin |
| Navigazione | Navigation Compose multiplatform |
| Stato | ViewModel + `StateFlow` |
| Date | `kotlinx-datetime`, fuso `Europe/Rome` |
| Config | BuildKonfig: `SUPABASE_URL`, `SUPABASE_ANON_KEY`; Gradle: `APP_VERSION`, `ANDROID_KEYSTORE_*`/`ANDROID_KEY_*` (firma). Tutto da `local.properties` / variabili CI |
| Grafica | `Canvas` di Compose per mappa e grafico (nessuna libreria di chart) |
| Font | Saira Condensed, Instrument Sans in Compose Resources |
| Persistenza locale | Nessuna (oltre alla sessione auth gestita da `supabase-kt`) |

## Struttura del codice

Un solo modulo `composeApp`. Package `it.manu.fuoriorario`, organizzato per funzionalità:

```
composeApp/src/
  commonMain/kotlin/it/manu/fuoriorario/
    App.kt                 # tema + Koin, header, login → informativa → home
    core/                  # SupabaseClient, Week/Season, utilità date
    domain/                # modelli puri + calcoli (Zones, Stats, Progress) — senza dipendenze
    data/                  # interfacce repository + implementazioni Supabase
    ui/theme/              # token colore/tipografia del prototipo
    ui/components/         # Toast, BottomSheet, Stepper, Pill, SegmentedControl
    ui/auth/               # login OTP, informativa
    ui/home/               # barra schede per ruolo + NavHost
    ui/shots/              # diario di tiro, mappa, grafico, registra sessione
    ui/plan/               # piano settimanale, editor esercizio, nota
    ui/roster/             # rosa (staff)
    ui/team/               # quadro squadra (staff)
    ui/settings/           # libreria + riferimenti (staff)
  commonMain/composeResources/   # strings.xml (IT), font
  commonTest/              # unit test domain + UI test (runComposeUiTest) con repository finti
  androidMain/ iosMain/ wasmJsMain/   # entry point + engine Ktor
iosApp/                    # progetto Xcode del wizard
supabase/
  migrations/              # schema + RLS versionati
  seed.sql                 # squadra, primo staff, libreria e riferimenti di default
  tests/                   # test pgTAP delle RLS
```

Regole:
- `domain/` non importa nulla di Compose né di Supabase: è il codice testato dagli unit test.
- Un'interfaccia per repository **solo** perché esistono due implementazioni (Supabase + finta per i test UI).
- Tutto il resto senza astrazioni aggiuntive.

## Schermate e navigazione

```
Login (email → codice OTP) → Informativa (primo accesso) → Home
Home = barra in basso:
  Diario di tiro  ─ foglio "Registra sessione"
  Piano           ─ fogli "Esercizio", "Nota"
  Squadra (staff) ─ Rosa, Impostazioni (Libreria, Riferimenti)
Header: menu giocatore (solo staff), logout
```

Layout a colonna singola, larghezza massima 560dp centrata (web e tablet).

## Modello dati (Postgres)

Ogni tabella con dati di squadra ha `team_id`. Chiavi `uuid` (`gen_random_uuid()`).

```sql
teams (
  id uuid pk,
  name text not null,
  zone_refs jsonb not null default '{"pit":0.55,"mls":0.40,"mlc":0.40,"mld":0.40,
     "acs":0.36,"als":0.33,"cen":0.33,"ald":0.33,"acd":0.36,"tl":0.70}',
  created_at timestamptz default now()
)

members (
  id uuid pk,
  team_id uuid fk teams,
  user_id uuid null fk auth.users unique,      -- valorizzato al primo login
  email citext not null,
  display_name text not null check (length between 1 and 40),
  jersey_number text null check (~ '^\d{1,2}$'),
  position text null,                            -- Playmaker | Guardia | Ala | Ala grande | Centro
  role text not null check (role in ('staff','player')),
  privacy_ack_at timestamptz null,
  created_at timestamptz default now(),
  unique (team_id, email)
)

shot_sessions (
  id uuid pk,
  team_id uuid fk teams,
  member_id uuid fk members on delete cascade,
  date date not null check (date <= current_date + 1),
  zones jsonb not null check (valid_zones(zones)),   -- {"pit":[made,att], ...}, solo zone con att > 0
  note text null check (length <= 300),
  created_by uuid fk auth.users,
  created_at timestamptz default now()
)

exercise_library (
  id uuid pk, team_id uuid fk teams,
  title text not null, category text not null, volume text, description text, video_url text,
  sort int not null default 0
)

plan_items (
  id uuid pk, team_id uuid fk teams,
  member_id uuid fk members on delete cascade,
  week date not null check (extract(isodow from week) = 1),   -- lunedì
  title text not null, category text not null, volume text, description text,
  video_url text check (video_url is null or video_url ~ '^https://'),
  days smallint[] not null check (cardinality(days) > 0 and days <@ '{0,1,2,3,4,5,6}'),
  sort int not null default 0
)

weekly_notes (
  member_id uuid fk members on delete cascade, week date, team_id uuid fk teams,
  note text not null check (length <= 500),
  primary key (member_id, week)
)

plan_checks (
  plan_item_id uuid fk plan_items on delete cascade,
  day smallint check (day between 0 and 6),
  team_id uuid fk teams,
  checked_at timestamptz default now(),
  primary key (plan_item_id, day)
)
```

Note:
- `valid_zones(jsonb)`: funzione `immutable` che verifica chiavi tra le 10 zone, `0 ≤ made ≤ attempted`, `attempted > 0`, almeno una zona. Validazione ripetuta lato client per i messaggi.
- `zones` in JSON (come nel prototipo): una sessione = un insert atomico; le zone sono fisse.
- Le statistiche si calcolano **lato client** in `domain/` sulle sessioni del periodo: volumi di una squadra sono piccoli.
- `date` è una data locale (Europe/Rome), senza orario.

## Autenticazione e collegamento membro

1. Lo staff crea il membro con l'email.
2. L'utente chiede l'OTP. Un **Before User Created auth hook** (funzione Postgres) rifiuta le email non presenti in `members` → nessun account orfano. Verificato in M1: GoTrue risponde 403 con `msg: "not_a_member"`. In cloud l'hook si attiva con `supabase config push`.
3. Trigger `after insert on auth.users`: `update members set user_id = new.id where email = new.email and user_id is null`.
4. Client: legge il proprio `members` → ruolo e `privacy_ack_at`; se null mostra l'informativa e chiama RPC `ack_privacy()`.

## Permessi (RLS)

Funzioni helper `security definer stable`:
- `my_member()` → riga `members` dell'utente corrente.
- `is_staff(team uuid)` → l'utente è staff di quella squadra.

| Tabella | select | insert | update | delete |
|---|---|---|---|---|
| teams | membri della squadra | — (seed) | staff | — |
| members | staff della squadra; il giocatore solo la propria riga | staff | staff | staff |
| shot_sessions | staff; giocatore proprie | staff; giocatore con `member_id` proprio | — | staff; giocatore proprie |
| exercise_library | membri della squadra | staff | staff | staff |
| plan_items, weekly_notes | staff; giocatore proprie | staff | staff | staff |
| plan_checks | staff; giocatore proprie | solo giocatore proprietario dell'item | — | solo giocatore proprietario |

`ack_privacy()` è l'unica scrittura del giocatore su `members` (RPC, aggiorna solo `privacy_ack_at`).
Le RLS sono la vera barriera: la UI nasconde, il database impedisce. Coperte da test pgTAP in `supabase/tests/`.

## Gestione errori e dati

- Nessuna cache: ogni schermata carica all'apertura; pull-to-refresh.
- Errore di rete in salvataggio → toast "Salvataggio non riuscito. Riprova tra poco.", il foglio resta aperto con i dati.
- Errore di permesso (RLS) → toast dedicato; non dovrebbe accadere se la UI rispetta i ruoli.

## Ambienti

| Ambiente | Supabase | Client |
|---|---|---|
| Sviluppo | locale (`supabase start`, Docker) | build debug, URL locale |
| Produzione | progetto cloud, regione UE (Francoforte) | build da tag su `main` |

Le migrazioni si applicano in produzione con `supabase db push` dal job di rilascio (o a mano durante la prova).

## Test

| Livello | Dove | Cosa | In CI |
|---|---|---|---|
| Unit | `commonTest` | `domain/`: percentuali, somma zone, periodi/stagione, classe zona, completamento, settimane | ✓ |
| UI | `commonTest` con `runComposeUiTest` + repository finti | 1 login OTP · 2 registra sessione (validazione segnati ≤ tentati) · 3 spunta esercizio · 4 staff aggiunge giocatore · 5 staff assegna esercizio dalla libreria | iOS Simulator + Wasm (browser headless). Android in locale |
| DB | `supabase/tests` (pgTAP) | RLS: giocatore non legge/scrive dati altrui, solo staff gestisce rosa/piani, solo giocatore spunta | ✓ (Supabase locale in CI) |

## CI/CD (GitHub Actions)

- **PR verso `develop` / `main`**: ktlint, build Android + iOS + Wasm, unit test, UI test (iOS sim + Wasm), test pgTAP.
- **Tag `vX.Y.Z` su `main`**: deploy Wasm su GitHub Pages, APK firmato su GitHub Releases.
- Branching gitflow: vedi [adr/0007](adr/0007-gitflow-e-distribuzione.md).
