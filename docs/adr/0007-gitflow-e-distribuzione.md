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

## Conseguenze
- Gli aggiornamenti Android vanno reinstallati a mano; Play Console (test interno) in M8.
- La keystore di firma va conservata fuori dal repo (secret CI): perderla impedisce gli aggiornamenti.
