# Fuori Orario — Glossario

Linguaggio comune del progetto. UI e documentazione in italiano, codice in inglese: ogni termine riporta il nome usato nel codice.

| Termine (IT) | Codice | Significato |
|---|---|---|
| Squadra | `Team` | Il gruppo che usa l'app. Nell'MVP ne esiste una sola, ma ogni dato porta un `team_id`. |
| Membro | `Member` | Una persona in una squadra: email, nome visualizzato, ruolo. Esiste prima del suo account (lo crea lo staff). |
| Staff | `Role.STAFF` | Membro che gestisce rosa, piani, libreria e riferimenti. Vede i dati di tutti. Può essere più di uno. |
| Giocatore | `Role.PLAYER` | Membro che registra i propri tiri e spunta il proprio piano. Vede solo i propri dati. |
| Rosa | `Roster` | L'elenco dei membri della squadra: i giocatori e, a parte, lo staff. Lo staff la gestisce nella scheda Squadra. |
| Togliere (un membro) | `remove` | Lo elimina dalla squadra con tutti i suoi dati e il suo account. Doppio tocco "Togli" → "Conferma". La squadra deve restare con almeno uno staff. |
| Nome visualizzato | `displayName` | Testo libero (anche soprannome, es. "Luca B."). Unico dato anagrafico obbligatorio oltre all'email. |
| Numero | `jerseyNumber` | Numero di maglia, facoltativo. |
| Ruolo in campo | `position` | Playmaker, Guardia, Ala, Ala grande, Centro. Facoltativo. Da non confondere con *ruolo* staff/giocatore. |
| Account | `auth.users` | Identità Supabase creata al primo login OTP; si collega al membro con la stessa email. |
| Informativa | `privacyAckAt` | Pagina privacy mostrata al primo accesso; si registra quando il membro la accetta. |
| Diario di tiro | `ShotLog` | Scheda con mappa, statistiche, andamento e sessioni di un giocatore. |
| Sessione di tiro | `ShotSession` | Un allenamento individuale registrato: data, tiri per zona, nota facoltativa. |
| Zona | `Zone` | Una delle 10 aree di tiro fisse: pitturato, 3 di media, 5 da tre, tiri liberi. Non configurabili. |
| Segnati / Tentati | `made` / `attempted` | Per ogni zona di una sessione. Vincolo: `made ≤ attempted`. |
| Riferimento | `zoneRefs` | Percentuale attesa per zona (`Map<Zone, Int>`, in DB `teams.zone_refs` come frazioni), per squadra, modificabile dallo staff (default: pitturato 55%, media 40%, tripla angolo 36%, tripla 33%, liberi 70%). |
| Classe zona | `ZoneHeat` | `HOT` (≥110% del riferimento), `EVEN`, `COLD` (<85%), `NONE` (nessun tiro). Colora la mappa. |
| Dal campo | `fieldGoal` | Tutte le zone escluso i tiri liberi. |
| Da tre | `three` | Le 5 zone oltre l'arco (triple). |
| Tiri presi | `attempted` | Tentati in tutte le zone, liberi compresi. |
| Periodo | `Period` | Filtro statistiche: 7 giorni, 30 giorni, Stagione. |
| Stagione | `Season` | Dal 1° settembre dell'anno N al 31 agosto dell'anno N+1. |
| Andamento | `Trend` | Grafico delle percentuali (dal campo e liberi) per sessione, ultime 14. |
| Settimana | `Week` | Lunedì–domenica, identificata dalla data del lunedì. Fuso Europe/Rome. |
| Piano settimanale | `WeeklyPlan` | Esercizi e nota assegnati a un giocatore per **una** settimana specifica. |
| Esercizio (del piano) | `PlanItem` | Copia di un esercizio con titolo, area, volume, descrizione, video, giorni. Modificarlo non tocca la libreria. |
| Libreria | `LibraryExercise` | Esercizi modello della squadra, gestiti dallo staff in Squadra → Impostazioni; scorciatoia per riempire un piano. Ordine scelto dallo staff (↑ ↓). |
| Eliminare (dalla libreria) | `removeFromLibrary` | Toglie l'esercizio modello: doppio tocco "Elimina" → "Conferma eliminazione". I piani che lo avevano copiato restano invariati. |
| Impostazioni | `TeamSection.SETTINGS` | Terza vista della scheda Squadra (staff), dopo Quadro e Rosa: Libreria, poi Riferimenti. |
| Area | `Category` | Ball handling, Tiro, Footwork, Atletica, Difesa, Recupero. |
| Volume | `volume` | Quantità testuale (es. "3 × 20"). |
| Spunta | `PlanCheck` | Il giocatore dichiara fatto un esercizio in un giorno. Solo il giocatore può spuntare. |
| Completamento | `Progress` | Spunte / giorni assegnati nella settimana. |
| Nota dello staff | `WeeklyNote` | Obiettivo o indicazione per il giocatore in quella settimana. |
| Copia settimana | `copyPreviousWeek` | Duplica esercizi (non spunte) della settimana precedente in quella mostrata. |
| Quadro squadra | `TeamOverviewScreen`, `OverviewRow` | Prima vista della scheda Squadra (staff): per giocatore completamento del piano della settimana corrente (verde ≥75%, arancio ≥40%, grigio sotto), tiri 7g, liberi 30g, triple 30g. Tocco su una riga → suo Diario di tiro. |
| Schema | `Play` | Un gioco della squadra disegnato dallo staff: titolo, categoria, descrizione, metà campo o campo intero, con o senza difesa, passi. Lo vede tutta la squadra nella scheda Schemi. |
| Categoria (dello schema) | `PlayCategory` | Attacco, Contro zona, Rimessa laterale, Rimessa dal fondo, Fine partita, Transizione, Difesa. Fisse. Da non confondere con *Area* degli esercizi. |
| Pedina | `piece` | Attaccanti `1`–`5`, difensori `X1`–`X5` (solo schemi con difesa), palla in mano a un attaccante. |
| Passo | `Step` | Le posizioni di tutte le pedine in un momento dello schema, chi ha la palla, chi blocca, le curve, una nota facoltativa (≤ 200). Massimo 20 per schema. |
| Movimento | `Move` | Ricavato confrontando un passo col precedente: taglio (continua), palleggio (ondulata), passaggio (tratteggiata, dritta), blocco (finisce con ⊥), difensore (altro colore). |
| Blocco | `screen` | Attaccante che in quel passo porta un blocco: il suo movimento finisce con ⊥; se è fermo, ha solo la ⊥ girata verso la palla. |
| Editor (degli schemi) | `PlayEditor` | Dove lo staff crea, modifica ed elimina uno schema dal telefono. Parte dalla disposizione di default (1 in punta, 2–3 ali, 4–5 post; con difesa ogni Xn tra n e il canestro). Il dito prende la pedina o la maniglia più vicina, se è a portata (altrimenti la pagina scorre); tenere premuto un attaccante gli dà la palla. "+ Passo" copia il passo corrente. Si salva tutto lo schema in una volta; uscire con modifiche (anche con il tasto indietro) o eliminare chiede il doppio tocco. Il campo si sceglie solo alla creazione. |
| Maniglia | `handles` | Pallino a metà di un movimento: trascinandolo il movimento si curva passando di lì; riportato al centro torna dritto. Spostando gli estremi o eliminando un passo, la curva continua a passare dalla maniglia. |
| Partita | `Game` | Una gara della squadra registrata dallo staff: data, avversario, casa/trasferta, nota facoltativa, convocati, eventi. Si salva tutta a fine partita e poi si può solo eliminare. |
| Convocati | `callUps` | I membri della rosa scelti per una partita. Nessun minimo né massimo. |
| Evento | `GameEvent` | Una cosa successa in partita, con giocatore, quarto e ordine: `TIRO` (zona, segnato/sbagliato), `LIBERO` (segnato/sbagliato), `RIM`, `AST`, `PP`, `REC`, `FAL`, `AVV` (punti dell'avversario, 1/2/3, senza giocatore). |
| Quarto | `Quarter` | 1–4, più `OT` (SUPP) che raccoglie tutti i supplementari. |
| Live | `GameLive` | Schermata dello staff a bordo campo: si tocca un convocato (resta selezionato), poi la zona o l'azione. "Annulla ultimo" ed elenco eventi eliminabili. |
| Partita in corso | `GameDraft` | La partita non ancora salvata, tenuta sul dispositivo a ogni tocco. Una per dispositivo; si cancella al logout. |
| Tabellino | `BoxScore` | Riga per convocato: PT, T2, T3, TL (segnati/tentati), RIM, AST, PP, REC, FAL. Il giocatore vede solo la propria. |
| Parziali | `quarterScores` | Punti delle due squadre per quarto. |
