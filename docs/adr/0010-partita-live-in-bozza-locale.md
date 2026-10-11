# 0010 — Partita live in bozza locale

**Stato**: accettato · 2026-10-09

## Contesto
Lo staff registra le partite live a bordo campo (M7.5), senza appunti da trascrivere. In palestra la rete è spesso scarsa e l'[ADR 0004](0004-solo-online-senza-realtime.md) non prevede dati locali: un evento perso a ogni caduta di rete renderebbe il live inutilizzabile.

## Decisione
- Eccezione circoscritta all'ADR 0004: la **partita in corso** (`GameDraft`) vive nel ViewModel ed è copiata come JSON in `multiplatform-settings` a ogni tocco. Sopravvive alla chiusura dell'app. Una sola per dispositivo.
- "Termina partita" la salva **tutta in una volta** con una RPC atomica (partita, convocati, eventi). Se fallisce: toast, bozza intatta, si riprova. Nessuna sincronizzazione durante la partita.
- Al logout la bozza si cancella, previa conferma: un dispositivo condiviso non passa dati a un altro account.
- Si salva il **registro degli eventi** (giocatore, tipo, zona, esito, valore, quarto, ordine), non i totali: tabellino, parziali e mappa si calcolano in `domain/`, come le statistiche del Diario. Il punteggio delle due squadre si salva anche sulla partita, così l'elenco non deve leggere gli eventi e il giocatore vede il risultato pur leggendo solo i propri eventi (RLS).
- Una partita salvata non si modifica: si elimina per intero.

## Conseguenze
- Il live funziona senza rete; i dati sono al sicuro solo dopo il salvataggio.
- Finché non è salvata, la partita esiste solo su quel dispositivo: non si vede altrove e non si passa a un altro staff.
- Togliere un membro elimina i suoi eventi (cascade); il punteggio salvato sulla partita resta, e può non coincidere più con il tabellino.
