# 0008 — Formato degli schemi

**Stato**: accettato · 2026-10-07

## Contesto
Lo staff disegna gli schemi dal telefono (M6.5) e tutta la squadra li rivede animati. Uno schema è una sequenza di posizioni; i movimenti fra una e l'altra vanno mostrati senza che lo staff li disegni a mano.

## Decisione
- Una riga di `plays` per schema: titolo, categoria (7 fisse), descrizione, campo `HALF`/`FULL`, `defense` e i **passi** in un unico `jsonb`. Uno schema si salva e si legge in un colpo, come le zone di una sessione.
- Coordinate nel viewBox della mappa di tiro (500 unità = 15 m, canestro d'attacco in alto, `y` dalla linea di fondo), fino a 50 unità (~1,5 m) fuori dalle linee per le rimesse. Campo intero: `y` fino a 940, l'altro canestro in basso.
- Passo: `{"pos": {"1": {"x":250,"y":330}, …, "X1": …}, "ball": "1", "screens": ["5"], "curves": {"1": {"x":…,"y":…}}, "note": "…"}`. Attaccanti `1`–`5`, difensori `X1`–`X5` solo con difesa; la palla è sempre in mano a un attaccante.
- I movimenti **non si salvano**: si ricavano confrontando un passo col precedente (`domain/Play.kt`). Chi cambia posto si muove: difensore, blocco (se è in `screens`), palleggio (se aveva la palla), altrimenti taglio. Se cambia chi ha la palla, passaggio dritto fra le posizioni finali. `curves` dà il punto di controllo di una curva quadratica.
- Riproduzione: 1 s per passo, giocatori nei primi 0,6 s, passaggio negli ultimi 0,4 s.
- Vincoli nel database: da 1 a 20 passi, titolo ≤ 60, descrizione ≤ 400, nota ≤ 200. La forma interna dei passi la garantisce l'editor, non il database.

## Conseguenze
- Un solo formato per editor e visualizzatore; cambiare un passo non lascia frecce vecchie.
- Non si può disegnare un movimento che non cambia posizione (es. un blocco da fermo non ha la ⊥): si vedrà con l'editor se serve.
