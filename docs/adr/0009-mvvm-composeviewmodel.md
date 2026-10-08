# 0009 — MVVM con ComposeViewModel

**Stato**: accettato · 2026-10-08

## Contesto
Le schermate tengono dati e logica in `LaunchedEffect`/`mutableStateOf` e creano i repository con `remember { SupabaseXRepository() }`. Ogni schermata gestisce caricamento ed errori a modo suo, e i test devono pilotare tutta l'app per verificare la logica. `ARCHITECTURE.md` dichiarava Koin e ViewModel, ma il codice non li usava.

Lo standard dei progetti aziendali Android è quello di [gestione-commessa-kernel](https://git.digitalbusinessolution.com/progetti-ai/gestione-commessa-kernel): MVVM, Koin, `ComposeViewModel` della libreria [sinetwork](https://git.digitalbusinessolution.com/mobile/android/libraries/sinetwork). Però sinetwork è una libreria solo Android e questa app gira anche su iOS e web.

## Decisione
- **MVVM come nel kernel.** Ogni schermata ha un `XViewModel` che estende `ComposeViewModel<XScreenState>`. La UI è divisa in tre livelli:
  1. `XScreen(vm)`: raccoglie lo stato e collega gli eventi.
  2. `XStateContent(state)`: fa un `when` su `UseCaseMutableState` (Loading / ShowData / Error).
  3. `XContent(...)`: è senza stato e riceve solo parametri e lambda.
- **`ComposeViewModel` copiato in `commonMain`**, non usato come dipendenza. Si copiano `core/viewmodel/` (`ComposeViewModel`, `UiState`, `UseCaseMutableState`, `DispatcherProvider`) e `core/error/` (`ErrorManager`, `ErrorHandler`, `ErrorHandlerWithRetry`, `FallbackHandler`). La base è il lifecycle ViewModel multipiattaforma di JetBrains. L'API pubblica resta identica a sinetwork; cambia solo ciò che è Android:
  - `ErrorManager` senza `Context`: i testi arrivano da Compose Resources, nell'`ErrorHandler` `@Composable` per i caricamenti e in `ErrorManager.checkError(e)` per le azioni (al posto di `checkError(e, errorManager.context)`);
  - `DispatcherProvider.io()` usa `Dispatchers.Default` su wasm, dove `Dispatchers.IO` non esiste;
  - `e::class.simpleName` al posto di `e::class.java`;
  - riconoscimento degli errori di rete da Ktor (`HttpRequestException`) e `kotlinx.io.IOException`, al posto di `okio.IOException`;
  - `checkError` (il testo del toast di un'azione fallita) non mostra mai `e.message`, che da Supabase arriva tecnico e in inglese: dà "La sessione è scaduta…" per un 401, "Non hai i permessi…" per `PermissionDeniedException` e "Salvataggio non riuscito…" per il resto, come l'app faceva già;
  - `println` al posto di `SILog`.
- **Errori.** I caricamenti usano `defaultLaunch`: in caso di errore, schermata intera con "Riprova" se l'errore è di rete. Le azioni (salva, spunta, elimina) usano `defaultLaunchForChannels`: l'errore arriva come toast (`LocalToast`) e lo stato resta `ShowData`. `AppErrorManager` valuta gli handler in questo ordine:
  1. `SessionExpiredErrorHandler` (401), senza riprova. Un 401, in un caricamento o in un'azione, fa chiamare ad `AppErrorManager` anche `SessionExpiry`, che cancella la sessione locale e riporta al login con l'avviso;
  2. `PermissionDeniedErrorHandler`;
  3. `NetworkErrorHandler`, con riprova;
  4. `FallbackHandler`.

  Le eccezioni di dominio, come `last_staff` e `plan_changed`, le gestisce lo stato della schermata, come `LoginOutcome` nel kernel.

  Eccezione: login e informativa mostrano anche l'errore generico sotto il campo, non in un toast. Era già così prima del rework, e lì non c'è un `LocalToast`.
- **JWT.** Niente `Authenticator` o `Interceptor` custom. `supabase-kt` aggiunge già il Bearer a ogni chiamata e rinnova il token da solo su tutte le piattaforme. Gli `Authenticator` e `Interceptor` di OkHttp esistono solo su Android, e il kernel non ne ha. `core/session` osserva `auth.sessionStatus`: se il rinnovo fallisce o la sessione è revocata, si torna al login con un avviso, come `SessionRevoked` nel kernel. `supabase-kt` cancella la sessione allo stesso modo sia all'uscita sia quando rifiuta il rinnovo (`NotAuthenticated(isSignOut = true)`): è scaduta ogni sessione cancellata senza che l'app abbia chiesto di uscire, anche all'avvio.
- **DI.** Koin con `koin-compose-viewmodel`.
- **Package per funzionalità**, come nel kernel:
  - `core/` contiene theme, designsystem, navigation, error, session e viewmodel;
  - ogni `feature/<x>/` contiene schermate, ViewModel e `data/` (interfaccia del repository e implementazione Supabase);
  - `domain/` resta puro e condiviso.
- **Stato.**
  - Nel ViewModel: i dati e le bozze dei form, come `data class` nello `ScreenState` con metodi `onXChanged`.
  - In `remember`: solo lo stato visivo (gesto in corso, animazione, scroll, foglio aperto o chiuso).
  - Nessuna libreria per i form (siform è solo Android).
- **Stato condiviso.** `MainViewModel` alla radice gestisce sessione, membro, informativa e instradamento. Il giocatore scelto dallo staff è il singleton Koin `SelectedPlayer`, un `StateFlow` salvato con `multiplatform-settings`.
- **Regole verificate dai test.** Konsist in `androidUnitTest`, che analizza `commonMain`, e `verify()` sui moduli Koin. Le regole sono in `AGENTS.md`.

## Alternative scartate
- **Dipendere da sinetwork:** è un AAR Android, quindi non compila per iOS e Wasm.
- **Un `ComposeViewModel` riscritto con un'API nuova:** chi passa dal kernel a questo progetto troverebbe nomi diversi per le stesse cose.
- **Plugin Ktor per il token:** duplica quello che `supabase-kt` fa già e rischia due refresh in parallelo.
- **Molecule, MVI o Decompose:** non sono lo standard aziendale.

## Conseguenze
- Se sinetwork diventerà multipiattaforma, basterà sostituire la copia e cambiare gli import.
- Una correzione a `ComposeViewModel` in sinetwork va riportata qui a mano.
- La migrazione è incrementale (issue #50–#56). Fino alla fine convivono il vecchio `ui/` e il nuovo `feature/`, e Konsist esclude i package non ancora migrati.
- Il rework precede la v0.1.0 (#21).
