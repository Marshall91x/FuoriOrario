# Fuori Orario — Product Requirements (MVP)

Termini in [CONTEXT.md](../CONTEXT.md). Riferimento visivo: [prototype/fuori-orario.html](prototype/fuori-orario.html).

## Obiettivo

Dare a staff e giocatori di una squadra di basket uno strumento per seguire il **lavoro individuale fuori dagli allenamenti**: quanto e come tira ogni giocatore, e se segue il piano assegnato dallo staff.

## Fasi

1. **Prova interna** con la squadra di Emanuele (giocatori dai 14 anni in su): web per tutti, APK Android.
2. **Store** (Google Play, App Store): dopo il feedback della prova. Vedi [ROADMAP.md](ROADMAP.md) M8.

## Utenti e permessi

| Azione | Giocatore | Staff |
|---|---|---|
| Vedere diario e piano | solo i propri | di tutti |
| Registrare una sessione di tiro | per sé | per qualunque giocatore |
| Eliminare una sessione | le proprie | tutte |
| Spuntare un esercizio | i propri | — |
| Assegnare/modificare esercizi e nota | — | ✓ |
| Gestire rosa e staff | — | ✓ |
| Modificare libreria e riferimenti | — | ✓ |
| Quadro squadra | — | ✓ |

## Funzionalità MVP

### F1 — Accesso
- Login con **codice OTP via email** (6 cifre). Nessuna password.
- Solo le email già inserite dallo staff possono accedere; al primo login l'account si collega al membro.
- Email sconosciuta → messaggio "Non sei in nessuna squadra, contatta lo staff".
- Al primo accesso: **informativa privacy** di una pagina con "Ho letto"; senza accettazione non si prosegue.
- Logout.

### F2 — Rosa (staff)
- Aggiungere un membro: email (obbligatoria), nome visualizzato (obbligatorio), numero e ruolo in campo (facoltativi), ruolo staff/giocatore.
- Modificare un membro; togliere un membro (con conferma) → i suoi dati vengono eliminati.
- Rosa vuota → invito a inserire i giocatori.

### F3 — Diario di tiro
- Lo staff sceglie il giocatore da un menu; il giocatore vede direttamente il proprio.
- **Statistiche** per periodo (7 giorni / 30 giorni / Stagione): tiri presi, % dal campo, % da tre, % liberi.
- **Mappa di tiro** sul mezzo campo FIBA, 9 zone + riquadro tiri liberi, colorate per classe rispetto al riferimento; tocco su una zona → dettaglio (segnati/tentati, %, riferimento).
- **Andamento**: grafico % dal campo e % liberi per sessione (ultime 14, minimo 2 sessioni).
- **Sessioni**: elenco dalla più recente, con eliminazione a doppio tocco di conferma.
- **Registra sessione** (foglio modale): data (default oggi, non futura), segnati/tentati per zona con stepper (+1 segnati, +5 tentati; segnati > tentati alza i tentati), nota facoltativa (max 300).
  - Validazione: segnati ≤ tentati per ogni zona; almeno una zona con tentativi.
  - Se il salvataggio fallisce il modulo resta compilato e si può riprovare.

### F4 — Piano settimanale
- Navigazione per settimana (‹ ›), "Questa settimana" evidenziata, giorno corrente evidenziato.
- Barra di completamento (spunte / giorni assegnati).
- Nota dello staff per la settimana.
- Per ogni esercizio: area, titolo, volume, descrizione, link video (https), 7 pallini giorno (assegnato / riposo / fatto).
- Giocatore: spunta e de-spunta i giorni assegnati.
- Staff: aggiunge esercizio (dalla libreria o libero), modifica, rimuove (con conferma), scrive la nota, **copia dalla settimana precedente** (esercizi senza spunte).
- Validazione: almeno un giorno; link video deve iniziare con `https://`.

### F5 — Impostazioni squadra (staff)
- **Libreria esercizi**: aggiungere, modificare, eliminare, riordinare. Default: i 10 esercizi del prototipo.
- **Riferimenti**: percentuale per ognuna delle 10 zone. Default del prototipo.

### F6 — Quadro squadra (staff)
- Tabella giocatori: completamento piano settimana corrente (pill verde ≥75%, arancio ≥40%, grigio sotto), tiri ultimi 7g, % liberi 30g, % triple 30g.
- Tocco su un giocatore → apre il suo diario.

### Trasversali
- Tema chiaro/scuro automatico, design del prototipo.
- Solo italiano (testi in Compose Resources).
- Dati ricaricati all'apertura delle schermate + pull-to-refresh.
- Toast di conferma/errore come nel prototipo.

## Fuori perimetro MVP
- Funzionamento offline, aggiornamento in tempo reale.
- Più squadre nella UI, creazione squadra dall'app (si fa con seed).
- Stagioni passate consultabili (prevista per settembre 2027).
- Notifiche, statistiche partita, export.
- Eliminazione account in-app, privacy policy pubblica, social login (→ M8).
- Distribuzione iOS nativa ai tester (nessun account Apple Developer: i tester iPhone usano il web).

## Dati e privacy
- Titolare: Emanuele Del Monte. Dati raccolti: email, nome visualizzato, numero/ruolo (facoltativi), sessioni di tiro, piani e spunte. Niente foto, data di nascita o altro.
- Database Supabase in regione UE.
- Cancellazione su richiesta, gestita manualmente durante la prova.
- Dettagli in [adr/0006](adr/0006-dati-minimi-e-privacy.md).

## Criteri di successo della prova
- Tutti i giocatori riescono ad accedere e registrare una sessione senza aiuto.
- Lo staff assegna i piani settimanali per almeno 4 settimane consecutive.
- Raccolta del feedback per decidere M8.
