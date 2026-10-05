# 0006 — Dati minimi e privacy per la prova interna

**Stato**: accettato · 2026-10-05 · *non è una consulenza legale*

## Contesto
Utenti reali, inclusi minorenni dai 14 anni (età del consenso digitale in Italia). Titolare del trattamento: Emanuele Del Monte. Il GDPR si applica anche a una prova interna.

## Decisione
- Dati raccolti: email e nome visualizzato (obbligatori, anche soprannome); numero e ruolo in campo facoltativi; sessioni di tiro, piani, spunte. Niente data di nascita, foto o altro.
- Supabase in regione UE.
- Informativa di una pagina al primo accesso con accettazione registrata (`privacy_ack_at`): chi tratta, quali dati, perché, dove, come chiedere la cancellazione.
- Cancellazione su richiesta via email, eseguita a mano; togliere un membro elimina i suoi dati (cascade).

## Rimandato a M8
Privacy policy pubblica, eliminazione account in-app, revisione dei consensi e del target d'età per gli store.
