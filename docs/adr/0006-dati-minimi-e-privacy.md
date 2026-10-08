# 0006 — Dati minimi e privacy per la prova interna

**Stato**: accettato · 2026-10-05 · *non è una consulenza legale*

## Contesto
Utenti reali, inclusi minorenni dai 14 anni (età del consenso digitale in Italia). Titolare del trattamento: Emanuele Del Monte. Il GDPR si applica anche a una prova interna.

## Decisione
- Dati raccolti: email e nome visualizzato (obbligatori, anche soprannome); numero e ruolo in campo facoltativi; sessioni di tiro, piani, spunte. Niente data di nascita, foto o altro.
- Supabase in regione UE (Francoforte). Le email con il codice OTP passano da Resend (SMTP), che riceve solo indirizzo e codice (dominio in regione UE).
- Informativa di una pagina al primo accesso con accettazione registrata (`privacy_ack_at`): chi tratta e come contattarlo (anima91x@gmail.com, pubblico nel repo come il nome), quali dati, perché e su che base (consenso), per quanto (finché si è in squadra), dove, diritti di accesso, rettifica e cancellazione, reclamo al Garante.
- Cancellazione su richiesta via email, eseguita a mano; togliere un membro elimina i suoi dati (cascade) **e il suo account** (`auth.users`): non resta nessuna email di chi non è più in squadra. Se lo staff lo riaggiunge, al primo accesso riparte da un account nuovo, informativa compresa.

## Rimandato a M8
Privacy policy pubblica, eliminazione account in-app da parte del membro stesso, revisione dei consensi e del target d'età per gli store.
