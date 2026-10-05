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

Font Saira Condensed e Instrument Sans sotto licenza SIL OFL 1.1 (`licenses/`).
