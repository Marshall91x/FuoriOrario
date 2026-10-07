# Fuori Orario — Roadmap

Ogni milestone diventa una GitHub Milestone; ogni punto una issue. Branch `feature/*` da `develop`.

## M0 — Fondamenta
- Progetto dal wizard KMP (Android, iOS, Wasm), package `it.manu.fuoriorario`, nome "Fuori Orario".
- ktlint, `.gitignore`, BuildKonfig per URL/chiave Supabase.
- Design system: colori chiaro/scuro, font Saira Condensed + Instrument Sans, componenti base (bottone, pannello, segmented, bottom sheet, toast).
- Supabase locale: migrazione iniziale (tutte le tabelle, `valid_zones`, RLS), `seed.sql`, primi test pgTAP.
- GitHub Actions: build tre target + test; deploy Pages su tag.
- **Fatto quando**: app vuota con tema gira su Android, iOS sim e browser; CI verde.

## M1 — Accesso
- Login OTP (email → codice), hook che rifiuta email sconosciute, collegamento membro al primo login.
- Informativa al primo accesso + `ack_privacy()`.
- Caricamento ruolo, navigazione per ruolo (tab Squadra solo staff), logout.
- UI test 1 (login).
- **Fatto quando**: staff e giocatore di seed entrano e vedono le tab giuste.

## M2 — Rosa
- Elenco, aggiunta, modifica, rimozione membri (giocatori e staff) con conferma.
- Menu giocatore nell'header per lo staff.
- UI test 4.

## M3 — Diario di tiro
- Calcoli in `domain/` con unit test (periodi, stagione 1/9–31/8, somme, %, classi zona).
- Statistiche, mappa interattiva (Canvas + hit test zone), dettaglio zona, riquadro liberi.
- Grafico andamento.
- Elenco sessioni + eliminazione.
- Foglio "Registra sessione" con stepper e validazioni.
- UI test 2.

## M4 — Piano settimanale
- Navigazione settimane, completamento, nota staff.
- Esercizi con pallini giorno; spunta del giocatore.
- Staff: aggiungi (da libreria di default), modifica, rimuovi, copia dalla settimana precedente.
- UI test 3 e 5.

## M5 — Impostazioni squadra
- Libreria esercizi modificabile (CRUD + ordine).
- Riferimenti per zona modificabili.

## M6 — Quadro squadra
- Tabella staff con completamento, tiri 7g, liberi 30g, triple 30g; tocco → diario.

## M6.5 — Playbook
- Tab Schemi per tutti: elenco per categoria, visualizzatore passo per passo con movimenti animati (taglio, palleggio, passaggio, blocco).
- Editor per lo staff sul telefono: metà campo o campo intero, difesa facoltativa, pedine trascinabili, curve, note per passo.
- **Fatto quando**: lo staff disegna uno schema dal telefono e un giocatore lo rivede animato.

## M7 — Prova con la squadra
- Progetto Supabase cloud (UE), migrazioni + seed di produzione (squadra reale, staff).
- Testo informativa definitivo.
- Release `v0.1.0` via gitflow: web su GitHub Pages, APK firmato su GitHub Releases.
- Inserimento della rosa reale, raccolta feedback.

## M8 — Pronti per gli store
- Privacy policy pubblica, eliminazione account in-app (richiesta da Apple e Google).
- Revisione consensi e target d'età (regole Google Play Families se applicabili).
- Account Apple Developer + TestFlight, Google Play Console.
- Valutare social login (Google ⇒ anche Sign in with Apple), multi-squadra, offline, realtime, stagioni passate.
