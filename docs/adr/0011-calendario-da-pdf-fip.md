# 0011 — Calendario importato dal PDF FIP

**Stato**: accettato · 2026-11-05

## Contesto
La squadra vuole vedere gare programmate, risultati e classifica del campionato. La FIP non offre API: il calendario si scarica solo in PDF (esempio in `docs/Calendario.pdf`: 11 squadre, 110 gare). Scriverlo a mano non è pensabile. Non esistono librerie PDF comuni ad Android, iOS e web, e il testo semplice del PDF attacca i nomi delle due squadre e mette date e ore in un blocco separato dalle gare.

## Decisione
- Il PDF lo legge una **Supabase Edge Function** con pdf.js, usando le **coordinate** del testo per ricomporre le righe (giornata, numero gara, data e ora, casa, ospite, campo). La funzione restituisce un'anteprima e non scrive nulla.
- Lo staff conferma l'anteprima e segna la nostra squadra; la scrittura è una RPC atomica.
- Il **numero gara FIP** è la chiave: un nuovo import aggiorna data, ora e campo delle gare ritrovate, non tocca mai i risultati e non cancella niente (le gare sparite sono segnalate nell'anteprima).
- **Campionato = una fase FIP**: una nuova fase è un nuovo import e un nuovo campionato. Si mostra l'ultimo della stagione corrente.
- I risultati li inserisce lo staff a mano (o arrivano dal live per le nostre gare); la **classifica si calcola in `domain/`**, come le statistiche.
- **Gara → Partita** (#103): il collegamento sta sulla gara (`fixtures.game_id`), non sulla partita, così una partita salvata non cambia ([ADR 0010](0010-partita-live-in-bozza-locale.md)). "Inizia live" porta l'id della gara nella bozza; `save_game` collega la gara e, salvo pareggio, ci scrive il risultato come giocata. Il risultato resta modificabile a mano (vale il referto). Eliminare la partita scollega la gara, che tiene il risultato; eliminare gara o campionato non tocca la partita. Le partite salvate prima restano scollegate.

## Conseguenze
- Se la FIP cambia l'impaginazione, l'import si rompe: l'anteprima lo rende visibile e le gare restano modificabili a mano. Il test della funzione gira sul PDF d'esempio.
- Prima Edge Function del progetto: va nel deploy (`supabase functions deploy`) e nella CI.
- I risultati delle altre squadre dipendono dallo staff: la classifica è giusta quanto i risultati inseriti.
