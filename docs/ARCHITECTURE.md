# Fuori Orario — Architettura

> **Migrazione in corso** verso MVVM ([adr/0009](adr/0009-mvvm-composeviewmodel.md)): questo documento descrive lo stato di arrivo. Avanzamento nelle issue #50–#56; togliere questa nota alla chiusura dell'ultima.

Decisioni motivate in [adr/](adr/). Termini in [CONTEXT.md](../CONTEXT.md). Limiti noti in [NOTE.md](NOTE.md).

## Stack

| Area | Scelta |
|---|---|
| Linguaggio / UI | Kotlin Multiplatform + Compose Multiplatform (versioni del wizard KMP al momento della creazione) |
| Target | Android, iOS (arm64, simulatorArm64), Web `wasmJs` |
| Backend | Supabase (Postgres, Auth, RLS), regione UE |
| Client backend | `supabase-kt` (auth + postgrest), Ktor engine: OkHttp (Android), Darwin (iOS), Js (Wasm) |
| DI | Koin + `koin-compose-viewmodel` |
| Navigazione | Navigation Compose multiplatform |
| Stato | MVVM: `ComposeViewModel` (copiato da sinetwork, vedi [adr/0009](adr/0009-mvvm-composeviewmodel.md)) su lifecycle ViewModel multipiattaforma |
| Token | JWT gestito da `supabase-kt` (Bearer + refresh automatico); nessun interceptor custom |
| Date | `kotlinx-datetime`, fuso `Europe/Rome` |
| Config | BuildKonfig: `SUPABASE_URL`, `SUPABASE_ANON_KEY`; Gradle: `APP_VERSION`, `ANDROID_KEYSTORE_*`/`ANDROID_KEY_*` (firma). Tutto da `local.properties` / variabili CI |
| Grafica | `Canvas` di Compose per mappa e grafico (nessuna libreria di chart) |
| Font | Saira Condensed, Instrument Sans in Compose Resources |
| Persistenza locale | Solo la sessione auth gestita da `supabase-kt` e il giocatore scelto dallo staff (`multiplatform-settings`, la stessa libreria di `supabase-kt`) |

## Struttura del codice

Un solo modulo `composeApp`. Package `it.manu.fuoriorario`, organizzato per funzionalità:

```
composeApp/src/
  commonMain/kotlin/it/manu/fuoriorario/
    App.kt                 # tema + Koin + NavHost, guidato da MainViewModel
    MainViewModel.kt       # sessione, membro, informativa, instradamento login → informativa → home
    di/                    # moduli Koin
    core/
      viewmodel/           # ComposeViewModel, UiState, UseCaseMutableState, DispatcherProvider (da sinetwork)
      error/               # ErrorManager, handler, AppErrorManager
      session/             # sessione supabase-kt, sessione scaduta, SelectedPlayer
      navigation/          # rotte e barra schede per ruolo
      designsystem/        # Toast, BottomSheet, Stepper, Pill, SegmentedControl
      theme/               # token colore/tipografia del prototipo
      Supabase.kt Dates.kt # client Supabase, Week/Season, utilità date
    domain/                # modelli puri + calcoli (Zones, Stats, Progress, Play) — senza dipendenze
    feature/
      auth/                # login OTP, informativa       + data/AuthRepository
      shots/               # diario di tiro, mappa, grafico, registra sessione + data/ShotRepository
      plan/                # piano settimanale, editor esercizio, nota + data/PlanRepository
      plays/               # schemi: elenco, visualizzatore, editor (staff), campo + data/PlayRepository
      roster/              # rosa (staff)                + data/RosterRepository
      team/                # quadro squadra (staff)
      settings/            # libreria + riferimenti (staff)
  commonMain/composeResources/   # strings.xml (IT), font
  commonTest/              # unit test domain + ViewModel, UI test (runAppTest) con modulo Koin di fake
  androidUnitTest/         # Konsist (regole di architettura) + verify() dei moduli Koin
  androidMain/ iosMain/ wasmJsMain/   # entry point + engine Ktor
iosApp/                    # progetto Xcode del wizard
supabase/
  migrations/              # schema + RLS versionati
  seed.sql                 # squadra, primo staff, un giocatore, uno schema (libreria e riferimenti di default arrivano dallo schema)
  tests/                   # test pgTAP delle RLS
```

Una feature contiene `XScreen.kt` + `XViewModel.kt` per schermata e `data/` con interfaccia del repository e implementazione Supabase. Un repository sta nella feature che possiede le tabelle; le altre feature (es. `team`, `settings`) lo importano da lì.

Regole (operative e verificate da Konsist: vedi [AGENTS.md](../AGENTS.md)):
- `domain/` non importa nulla di Compose, Supabase né Koin: è il codice testato dagli unit test.
- Un'interfaccia per repository **solo** perché esistono due implementazioni (Supabase + finta per i test).
- Tutto il resto senza astrazioni aggiuntive.

## Schermate e navigazione

```
Login (email → codice OTP) → Informativa (primo accesso) → Home
Home = barra in basso:
  Diario di tiro  ─ foglio "Registra sessione"
  Piano           ─ fogli "Esercizio", "Nota"
  Schemi          ─ elenco per categoria → schema passo per passo (◀ ▶ Riproduci); staff: Nuovo schema / Modifica → editor
  Squadra (staff) ─ Quadro (tocco → Diario del giocatore), Rosa, Impostazioni (Libreria, Riferimenti)
Header: menu giocatore (solo staff), logout
```

Layout a colonna singola, larghezza massima 560dp centrata (web e tablet).

## Modello dati (Postgres)

Ogni tabella con dati di squadra ha `team_id`. Chiavi `uuid` (`gen_random_uuid()`).

```sql
teams (
  id uuid pk,
  name text not null,
  zone_refs jsonb not null check (valid_zone_refs(zone_refs)) default '{"pit":0.55,"mls":0.40,"mlc":0.40,"mld":0.40,
     "acs":0.36,"als":0.33,"cen":0.33,"ald":0.33,"acd":0.36,"tl":0.70}',   -- tutte le 10 zone, frazioni in (0, 1]
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
  title text not null, category category not null, volume text, description text, video_url text,   -- category: dominio condiviso con plan_items
  sort int not null default 0
)

plan_items (
  id uuid pk, team_id uuid fk teams,
  member_id uuid fk members on delete cascade,
  week date not null check (extract(isodow from week) = 1),   -- lunedì
  title text not null check (length(btrim) between 1 and 60),
  category text not null check (in le 6 aree), volume text check (length <= 40), description text check (length <= 400),
  video_url text check (video_url is null or video_url ~ '^https://'),
  days smallint[] not null check (cardinality(days) > 0 and days <@ '{0,1,2,3,4,5,6}'),   -- 0 = lunedì
  sort int not null default 0,
  created_at timestamptz default now()
)

weekly_notes (
  member_id uuid fk members on delete cascade, week date, team_id uuid fk teams,
  note text not null check (length <= 500),
  primary key (member_id, week)
)

plays (
  id uuid pk, team_id uuid fk teams,
  title text not null check (length(btrim) between 1 and 60),
  category text not null check (in le 7 categorie), description text check (length <= 400),
  court text not null check (court in ('HALF','FULL')), defense boolean not null default false,
  steps jsonb not null check (valid_play_steps(steps)),   -- 1–20 passi, note ≤ 200; formato in adr/0008
  created_at timestamptz default now(),
  check (defense or not play_has_defenders(steps))          -- X1–X5 solo con difesa
)

plan_checks (
  plan_item_id uuid fk plan_items on delete cascade,
  day smallint check (day between 0 and 6),        -- solo un giorno assegnato all'esercizio (RLS)
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
5. Togliere un membro (trigger `after delete on members`) cancella anche il suo account in `auth.users` (ADR 0006): se viene riaggiunto, al prossimo OTP si crea un account nuovo e il punto 3 lo ricollega. Il client, se non trova più la propria riga, esce.

## Permessi (RLS)

Funzioni helper `security definer stable`:
- `my_member()` → riga `members` dell'utente corrente.
- `is_staff(team uuid)` → l'utente è staff di quella squadra.

| Tabella | select | insert | update | delete |
|---|---|---|---|---|
| teams | membri della squadra | — (seed) | staff | — |
| members | staff della squadra; il giocatore solo la propria riga | staff | staff | staff |
| shot_sessions | staff; giocatore proprie | staff; giocatore con `member_id` proprio | — | staff; giocatore proprie |
| exercise_library, plays | membri della squadra | staff | staff | staff |
| plan_items, weekly_notes | staff; giocatore proprie | staff | staff | staff |
| plan_checks | staff; giocatore proprie | solo giocatore proprietario dell'item | — | solo giocatore proprietario |

`ack_privacy()` è l'unica scrittura del giocatore su `members` (RPC, aggiorna solo `privacy_ack_at`).
`copy_previous_week(player, target, seen)` copia in un colpo gli esercizi della settimana prima (RPC `security invoker`: valgono le RLS di `plan_items`). Se la settimana non ha più `seen` esercizi (un altro staff l'ha cambiata) non copia nulla e alza `plan_changed` (SQLSTATE `FO002`): due copie insieme non creano doppioni.
`set_zone_refs(refs)` sostituisce i riferimenti della squadra dell'utente (RPC `security invoker`: valgono le RLS di `teams`, solo lo staff scrive). Il client non conosce l'id della squadra; i default stanno anche in `domain/` (`DEFAULT_ZONE_REFS`) per il "Ripristina".
`reorder_library(ids)` mette la libreria nell'ordine dato con un solo `update` (RPC `security invoker`: valgono le RLS di `exercise_library`), così un errore non lascia due esercizi allo stesso posto.
Trigger `keep_one_staff` (`before update of role, team_id or delete on members`): una squadra non resta mai senza staff; togliere o declassare l'ultimo alza `last_staff` (SQLSTATE `FO001`). Cancellare l'intera squadra resta possibile.
Le RLS sono la vera barriera: la UI nasconde, il database impedisce. Coperte da test pgTAP in `supabase/tests/`.

## Gestione errori e dati

- Nessuna cache: ogni schermata carica all'apertura (`defaultLaunch` nel ViewModel); pull-to-refresh.
- Errore nel caricamento → schermata d'errore intera; "Riprova" solo per errori di rete.
- Errore in un'azione (`defaultLaunchForChannels`) → toast, la schermata resta coi dati. Rete: "Salvataggio non riuscito. Riprova tra poco.", il foglio resta aperto con i dati.
- Handler in ordine (`AppErrorManager`): sessione scaduta → permesso negato → rete → generico.
- Sessione scaduta o revocata (refresh fallito, osservato da `auth.sessionStatus`) → login con avviso.
- Errore di permesso (RLS) → toast dedicato; non dovrebbe accadere se la UI rispetta i ruoli. Update e delete filtrati dalla RLS non danno errore: il client chiede la riga indietro e, se non arriva, lo tratta come permesso negato.
- Errori di dominio (es. ultimo staff `FO001` → "Serve almeno un membro dello staff nella squadra.") gestiti dallo stato della schermata.

## Ambienti

| Ambiente | Supabase | Client |
|---|---|---|
| Sviluppo | locale (`supabase start`, Docker) | build debug, URL locale |
| Produzione | progetto cloud, regione UE (Francoforte) | build da tag su `main` |

Le migrazioni si applicano in produzione con `supabase db push` dal job di rilascio (o a mano durante la prova).

## Test

| Livello | Dove | Cosa | In CI |
|---|---|---|---|
| Unit | `commonTest` | ViewModel (con `TestDispatcherProvider` e repository finti) · `domain/`: percentuali, somma zone, periodi/stagione, classe zona, completamento, settimane, movimenti e interpolazione degli schemi | ✓ |
| UI | `commonTest` con `runAppTest` + repository finti | 1 login OTP · 2 registra sessione (validazione segnati ≤ tentati) · 3 spunta esercizio · 4 staff aggiunge giocatore · 5 staff assegna esercizio dalla libreria · staff gestisce la libreria (aggiunge, modifica, riordina, elimina) · staff modifica e ripristina i riferimenti, la mappa si ricolora · staff vede il quadro squadra e apre il diario di un giocatore · giocatore apre uno schema e va al passo successivo | iOS Simulator + Wasm (browser headless). Android in locale |
| DB | `supabase/tests` (pgTAP) | RLS: giocatore non legge/scrive dati altrui, solo staff gestisce rosa/piani/riferimenti, solo giocatore spunta | ✓ (Supabase locale in CI) |

Architettura: Konsist + `verify()` dei moduli Koin in `androidUnitTest`.

I test UI partono con `runAppTest` (non `runComposeUiTest`): carica prima tutte le stringhe, perché su Wasm ogni stringa letta la prima volta arriva in modo asincrono e per qualche frame l'etichetta è vuota.

## CI/CD (GitHub Actions)

- **PR verso `develop` / `main`**: ktlint, build Android + iOS + Wasm, unit test, UI test (iOS sim + Wasm), test pgTAP.
- **Tag `vX.Y.Z` su `main`**: deploy Wasm su GitHub Pages, APK firmato su GitHub Releases.
- Branching gitflow: vedi [adr/0007](adr/0007-gitflow-e-distribuzione.md).
