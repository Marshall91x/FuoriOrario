# 0002 — Supabase come backend

**Stato**: accettato · 2026-10-05

## Contesto
I dati sono condivisi tra staff e giocatori su dispositivi diversi; servono auth e permessi per riga. Il client gira anche su Wasm.

## Decisione
Supabase (Postgres + Auth + RLS) con `supabase-kt`, che supporta Android, iOS e Wasm. Regione UE. Permessi applicati con Row Level Security. `team_id` su ogni tabella fin da subito, anche se la UI gestisce una sola squadra.

Sviluppo su Supabase locale (CLI + Docker) con migrazioni versionate in `supabase/migrations/`; un progetto cloud per la produzione.

## Alternative scartate
- Firebase: l'SDK Kotlin GitLive non supporta Wasm.
- Backend proprio (Ktor): molto più lavoro per auth, permessi e hosting.

## Conseguenze
- La sicurezza vive nelle policy SQL → test pgTAP obbligatori.
- Piano gratuito sufficiente per la prova (attenzione: i progetti gratuiti inattivi vengono messi in pausa).
