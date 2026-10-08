# Fuori Orario

App per seguire il lavoro individuale dei giocatori di basket fuori dagli allenamenti: diario di tiro, piano settimanale, quadro squadra.
Kotlin Multiplatform + Compose (Android, iOS, Web Wasm) con backend Supabase.

- [Glossario](CONTEXT.md)
- [Requisiti MVP](docs/PRD.md)
- [Architettura](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisioni (ADR)](docs/adr/)
- [Prototipo](docs/prototype/fuori-orario.html)

## Sviluppo

Requisiti: JDK 21, Android SDK, Xcode (per iOS), Chrome (per i test web).

`local.properties` (non versionato):

```properties
sdk.dir=/percorso/Android/sdk
SUPABASE_URL=
SUPABASE_ANON_KEY=
```

In CI gli stessi valori arrivano da variabili d'ambiente. Mai committare chiavi.

### Supabase locale

Requisiti: Docker e [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started) (`brew install supabase/tap/supabase`).

```sh
supabase start          # Postgres + Auth + Mailpit; applica supabase/migrations e supabase/seed.sql
supabase db reset       # ricrea il database da migrazioni + seed
supabase test db        # test pgTAP delle RLS (supabase/tests)
supabase stop
```

`supabase status` mostra URL e chiave anonima: mettili in `local.properties` (`SUPABASE_URL=http://127.0.0.1:54321`, `SUPABASE_ANON_KEY=<anon key>`).

Account di prova del seed: `staff@example.com` (staff) e `giocatore@example.com` (giocatore). Il codice OTP arriva in Mailpit: <http://127.0.0.1:54324>. Altre email vengono rifiutate.

Il rifiuto delle email fuori rosa è l'auth hook `before_user_created` (`supabase/config.toml`): in un progetto cloud va attivato con `supabase config push` (o dalla dashboard), `db push` non basta.

Emulatore Android: `adb reverse tcp:54321 tcp:54321` per raggiungere `127.0.0.1` del Mac.

### Test

| Cosa | Comando |
|---|---|
| UI test iOS | `./gradlew :composeApp:iosSimulatorArm64Test` |
| UI test web | `./gradlew :composeApp:wasmJsBrowserTest` |
| UI test Android (emulatore acceso) | `./gradlew :composeApp:connectedDebugAndroidTest` |
| RLS | `supabase test db` |

| Target | Comando |
|---|---|
| Android | `./gradlew :composeApp:installDebug` |
| iOS | apri `iosApp/iosApp.xcodeproj` in Xcode ed esegui su simulatore |
| Web | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Stile | `./gradlew :composeApp:ktlintCheck` (correzione: `ktlintFormat`) |

## Rilascio

Un tag `vX.Y.Z` su un commit di `main` avvia `.github/workflows/release.yml`, che:

- pubblica la build web di produzione su GitHub Pages (`https://marshall91x.github.io/FuoriOrario/`);
- allega l'APK firmato `fuori-orario-vX.Y.Z.apk` alla GitHub Release del tag.

```sh
git switch main && git pull
git tag v0.1.0 && git push origin v0.1.0
```

`versionName` = `X.Y.Z`, `versionCode` = `X*10000 + Y*100 + Z` (Y e Z < 100), così ogni APK nuovo si installa sopra il precedente.

### Keystore di firma

Da generare **una sola volta** e conservare fuori dal repo: se si perde, gli aggiornamenti dell'APK non si installano più sopra la versione esistente (serve disinstallare).

```sh
keytool -genkeypair -keystore fuori-orario-release.jks -storetype PKCS12 \
  -alias fuori-orario -keyalg RSA -keysize 4096 -validity 10000
base64 -i fuori-orario-release.jks | pbcopy   # contenuto per il secret
```

Conserva il file `.jks` e la password in un password manager (con backup). In GitHub → Settings → Secrets and variables → Actions:

| Secret | Valore |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | keystore in base64 |
| `ANDROID_KEYSTORE_PASSWORD` | password della keystore |
| `ANDROID_KEY_ALIAS` | `fuori-orario` |
| `ANDROID_KEY_PASSWORD` | password della chiave (con PKCS12 è uguale alla precedente) |

Per firmare in locale, le stesse chiavi (con `ANDROID_KEYSTORE_PATH` al posto di `_BASE64`) vanno in `local.properties`; senza, `assembleRelease` produce un APK non firmato.

### GitHub Pages (una tantum)

1. Settings → Pages → Source: **GitHub Actions**.
2. Settings → Environments → `github-pages` → Deployment branches and tags: aggiungi la regola tag `v*` (di default è ammesso solo il branch di default).

### Supabase di produzione (una tantum)

1. Crea il progetto su supabase.com nella regione **Central EU (Frankfurt)**.
2. Su [Resend](https://resend.com) verifica il dominio del mittente e crea una API key.
3. In `supabase/config.toml` decommenta il blocco `[remotes.production]` e metti in `project_id` il ref del progetto (la parte prima di `.supabase.co` nell'URL).
4. Collega il progetto e applica migrazioni e config (hook delle email fuori rosa, SMTP, `site_url`, limite di invio email):

   ```sh
   supabase link --project-ref <ref>
   supabase db push                       # solo migrazioni: seed.sql è per lo sviluppo
   RESEND_API_KEY=<api key> SMTP_SENDER_EMAIL=accesso@<dominio> supabase config diff
   RESEND_API_KEY=<api key> SMTP_SENDER_EMAIL=accesso@<dominio> supabase config push
   ```

5. Copia `supabase/seed.production.example.sql` in `supabase/seed.production.sql` (non versionato), metti nome della squadra ed email dello staff, ed eseguilo una volta nel SQL Editor della dashboard. I giocatori li aggiunge lo staff da Rosa.
6. In GitHub → Settings → Secrets and variables → Actions imposta `SUPABASE_URL` e `SUPABASE_ANON_KEY` del progetto cloud: le usa la build di rilascio.

Prova: accedi con un'email dello staff e controlla che il codice arrivi da Resend; un'email fuori rosa deve essere rifiutata.

Font Saira Condensed e Instrument Sans sotto licenza SIL OFL 1.1 (`licenses/`).
