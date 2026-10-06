# Fuori Orario — Note e limiti noti

Scelte consapevoli che si discostano dal prototipo o lasciano un caso aperto. Per ognuna: cosa succede e quando rivederla.

## #12 — Staff segue qualunque giocatore

- **La scelta del giocatore non sopravvive al riavvio.** Resta tra le schede, ma riaprendo l'app lo staff riparte dal primo giocatore della rosa. Il prototipo la salvava in `localStorage` (`fo.pid`); l'architettura esclude la persistenza locale. Da rivedere se lo staff lo chiede: basta salvare l'id del giocatore scelto.
- **Cambiando giocatore il periodo torna a 30 giorni.** La schermata del Diario si ricrea per ogni giocatore, così sessioni e salvataggi in corso non passano da uno all'altro. Il prototipo teneva il periodo. Da rivedere spostando il periodo fuori dalla schermata.
- **Lo staff può registrare sessioni per sé.** La RLS conserva la regola "per sé" valida per tutti i membri; per gli altri membri lo staff può solo per i giocatori. La UI non lo permette. Da chiudere nella policy di insert se dovesse servire.
