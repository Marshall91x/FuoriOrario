# 0008 — Formato degli schemi

**Stato**: accettato · 2026-10-07

## Contesto
Lo staff disegna gli schemi dal telefono (M6.5) e tutta la squadra li rivede animati. Uno schema è una sequenza di posizioni; i movimenti fra una e l'altra vanno mostrati senza che lo staff li disegni a mano.

## Decisione
- Una riga di `plays` per schema: titolo, categoria (7 fisse), descrizione, campo `HALF`/`FULL`, `defense` e i **passi** in un unico `jsonb`. Uno schema si salva e si legge in un colpo, come le zone di una sessione.
- Coordinate nel viewBox della mappa di tiro (500 unità = 15 m, canestro d'attacco in alto, `y` dalla linea di fondo), fino a 50 unità (~1,5 m) fuori dalle linee per le rimesse. Campo intero: `y` fino a 940, l'altro canestro in basso.
- Passo: `{"pos": {"1": {"x":250,"y":330}, …, "X1": …}, "ball": "1", "screens": ["5"], "curves": {"1": {"x":…,"y":…}}, "note": "…", "passLast": true}` (`passLast` solo se vero). Attaccanti `1`–`5`, difensori `X1`–`X5` solo con difesa (`check` nel database); la palla è sempre in mano a un attaccante.
- I movimenti **non si salvano**: si ricavano confrontando un passo col precedente (`domain/Play.kt`). Chi cambia posto si muove: difensore, blocco (se è in `screens`), palleggio (se ha la palla a fine passo, cioè l'ha appena ricevuta o la teneva), altrimenti taglio (anche chi passa e poi si muove). Chi è in `screens` senza muoversi ha solo la ⊥, girata verso la palla. Se cambia chi ha la palla, passaggio dritto fra le posizioni **iniziali** di chi passa e di chi riceve (quelle del passo precedente): in un passo prima parte la palla, poi si muovono i giocatori (#91). Con `passLast` è il contrario, come sulla carta: prima si muovono i giocatori (chi passa palleggia, chi riceve taglia), poi passaggio fra le posizioni **finali**. Non lo sceglie il coach: l'editor mette `passLast` quando la palla viene data dopo aver spostato chi passa o chi riceve, e lo toglie se uno dei due viene spostato dopo (#98). `curves` dà il punto di controllo di una curva quadratica.
- Riproduzione: 1 s per passo, passaggio nei primi 0,4 s, giocatori negli ultimi 0,6 s; con `passLast` giocatori nei primi 0,6 s, passaggio negli ultimi 0,4 s. "Riproduci" va fino all'ultimo passo e, se ci si è già, riparte dal primo (#71); ◀ / ▶ lo fermano.
- Vincoli nel database: da 1 a 20 passi (senza passi non c'è nulla da mostrare), titolo ≤ 60, descrizione ≤ 400, nota ≤ 200, difensori solo con difesa. Il resto della forma dei passi la garantisce l'editor.

## Conseguenze
- Un solo formato per editor e visualizzatore; cambiare un passo non lascia frecce vecchie.
- La ⊥ di un blocco da fermo guarda la palla, non il compagno bloccato: per un blocco lontano dalla palla l'orientamento è approssimato.
