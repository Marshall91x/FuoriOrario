# 0007 — Gitflow e distribuzione

**Stato**: accettato · 2026-10-05

## Decisione
- **Gitflow**: `main` (produzione, tag `vX.Y.Z`), `develop` (integrazione), `feature/*` da `develop`, `release/*` da `develop` verso `main`, `hotfix/*` da `main`.
- PR obbligatorie con CI verde verso `develop` e `main`. Conventional commits (`feat:`, `fix:`, …), ktlint.
- Codice, nomi DB e commit in inglese; UI e documentazione in italiano.
- **Distribuzione prova** (dai tag su `main`):
  - Web Wasm su GitHub Pages: usato da tutti, inclusi i tester iPhone.
  - APK Android firmato su GitHub Releases (installazione manuale).
  - iOS: solo sul dispositivo dello sviluppatore via Xcode; nessun account Apple Developer fino a M8.

- **Repository pubblico** (deciso il 2026-10-05): con GitHub Free un repo privato non ha né GitHub Pages né protezione dei branch. Nel repo non ci sono segreti: l'URL e la chiave anon di Supabase sono pubblici per natura (i dati sono protetti dalle RLS, vedi 0002); chiave `service_role`, keystore e password restano nei secret della CI o in `local.properties`, mai nel repo.

## Alternative scartate
- Repo privato + Cloudflare Pages/Netlify: un servizio in più e nessuna protezione dei branch.
- GitHub Pro: costo ricorrente non giustificato.

## Conseguenze
- Il codice e la documentazione (incluso il nome del titolare del trattamento) sono visibili a chiunque.
- `main` e `develop` protetti: PR obbligatoria, niente force-push né cancellazione; CI verde obbligatoria da M0.
- Gli aggiornamenti Android vanno reinstallati a mano; Play Console (test interno) in M8.
- La keystore di firma va conservata fuori dal repo (secret CI): perderla impedisce gli aggiornamenti.
