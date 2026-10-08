# Regole per chi scrive codice (persone e agent)

Prima di toccare il codice leggi [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), il glossario [CONTEXT.md](CONTEXT.md) e le decisioni in [docs/adr/](docs/adr/). Lo standard MVVM è in [ADR 0009](docs/adr/0009-mvvm-composeviewmodel.md). Implementazione di riferimento: `feature/auth`.

## Architettura (verificata da Konsist in `androidUnitTest`)
- Ogni schermata ha un `XViewModel` che estende `ComposeViewModel` (`core/viewmodel`). I `*ViewModel` stanno in `feature..`, tranne `MainViewModel` che sta alla radice.
- I file `*Screen` stanno in `feature..`.
- Niente package per layer (`ui/`, `data/`, `viewmodel/` di primo livello). Fa eccezione solo `domain/`.
- `domain/` non importa Compose, Supabase né Koin.
- I moduli Koin passano `verify()`.

## Schermate
- La UI ha tre livelli:
  1. `XScreen(vm)` raccoglie lo stato e collega le lambda.
  2. `XStateContent(state)` fa un `when` su `UseCaseMutableState`.
  3. `XContent(...)` è senza stato, e lì vanno le `@Preview`.
- Una schermata di secondo livello (aperta con un tasto da una scheda) nasconde la barra in basso e gestisce il back di sistema con `BackHandler`; lo dice a `Home` con `onSecondLevel`, come `PlaysScreen` (ARCHITECTURE "Schermate e navigazione").
- Nessun `LaunchedEffect` per caricare o salvare dati: lo fa il ViewModel.
- Nessun repository creato in un composable: lo inietta Koin nel ViewModel.
- Nel ViewModel vanno i dati e le bozze dei form (`data class` nello `ScreenState`, metodi `onXChanged`). In `remember` va solo lo stato visivo: gesto in corso, animazione, scroll, foglio aperto o chiuso.
- I caricamenti usano `defaultLaunch`; in caso di errore c'è la schermata d'errore, con "Riprova" se l'errore è di rete.
- Le azioni (salva, spunta, elimina) usano `defaultLaunchForChannels`; in caso di errore c'è un toast (`LocalToast`) e i dati restano a schermo. Fanno eccezione login e informativa, che mostrano l'errore sotto il campo (ADR 0009).
- Le eccezioni di dominio (`mapErrors`) diventano stato della schermata, non un errore generico.
- Il giocatore scelto dallo staff si legge da `SelectedPlayer`, mai direttamente dai settings.
- Nessun `Authenticator` o `Interceptor` per il token: il JWT lo gestisce `supabase-kt`.

## `core/viewmodel` e `core/error`
Sono copiati da sinetwork. L'API pubblica non si cambia: gli adattamenti consentiti sono elencati in ADR 0009. Una correzione arrivata da sinetwork va riportata qui a mano.

## Test
- Ogni ViewModel ha test unitari in `commonTest` con `TestDispatcherProvider` e repository finti.
- I test UI usano `runAppTest` con il modulo Koin di test. Un refactor non cambia le asserzioni.
- Prima di una PR devono passare `./gradlew ktlintCheck` e i test.

## Processo
- Gitflow ([ADR 0007](docs/adr/0007-gitflow-e-distribuzione.md)): un branch `feature/<n>-<slug>` da `develop`, conventional commits.
- Codice, nomi nel database e commit sono in inglese; UI e documentazione in italiano.
- Un refactor non cambia ciò che si vede: un cambiamento visibile va in un'issue separata.
