# 0004 — Solo online, senza realtime né database locale

**Stato**: accettato · 2026-10-05

## Contesto
Il prototipo si aggiorna in tempo reale. In palestra la connessione può essere scarsa. L'offline-first con sincronizzazione è costoso, specie su Wasm.

## Decisione
- Nessun database locale, nessuna sincronizzazione offline.
- Nessun realtime: i dati si ricaricano all'apertura della schermata e con pull-to-refresh.
- Se un salvataggio fallisce, il modulo resta compilato e si può riprovare.

## Conseguenze
- Codice molto più semplice; nessun conflitto da risolvere.
- Da rivalutare in M8 se la squadra segnala problemi di connessione o se lo staff vuole il quadro aggiornato dal vivo.
