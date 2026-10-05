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

Font Saira Condensed e Instrument Sans sotto licenza SIL OFL 1.1 (`licenses/`).
