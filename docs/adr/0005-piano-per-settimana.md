# 0005 — Un piano per ogni settimana

**Stato**: accettato · 2026-10-05

## Contesto
Nel prototipo ogni giocatore ha un unico piano che si ripete ogni settimana, con le spunte salvate per settimana. Togliere un esercizio cancella anche lo storico delle spunte passate.

## Decisione
Gli esercizi (`plan_items`) appartengono a una settimana specifica (`week` = lunedì). La nota dello staff è per settimana. Lo staff può **copiare dalla settimana precedente** (solo esercizi, non spunte). Gli esercizi presi dalla libreria sono copie: modificare la libreria non cambia i piani esistenti.

## Conseguenze
- Storico esatto e consultabile settimana per settimana.
- Ogni settimana lo staff deve copiare o preparare il piano; si valuterà una copia automatica dopo la prova.
