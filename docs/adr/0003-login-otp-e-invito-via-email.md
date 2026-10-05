# 0003 — Login con OTP via email e invito tramite email del membro

**Stato**: accettato · 2026-10-05

## Contesto
Ogni giocatore ha un account personale e scrive solo sui propri dati. Il login deve funzionare uguale su Android, iOS e web. Utenti dai 14 anni in su.

## Decisione
- Login con codice OTP a 6 cifre via email (niente password, niente deep link).
- Lo staff inserisce l'email quando crea il membro; al primo login un trigger collega `auth.users` al membro con la stessa email.
- Le email non presenti in `members` vengono rifiutate (Before User Created hook; fallback `shouldCreateUser = false`).
- Squadra e primo staff creati con `seed.sql`; nessun flusso "crea squadra".

## Alternative scartate
- Magic link: richiede deep link su mobile e redirect sul web.
- Social login: Google su iOS impone anche Sign in with Apple; rimandato a M8.
- Codice squadra condiviso: chiunque potrebbe scrivere a nome di altri.

## Conseguenze
- Dipendenza dall'invio email di Supabase: il mailer integrato ha limiti bassi; in produzione configurare un SMTP (es. Resend/Brevo) prima di M7.
