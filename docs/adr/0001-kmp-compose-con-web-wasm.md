# 0001 — Kotlin Multiplatform + Compose con web Wasm

**Stato**: accettato · 2026-10-05

## Contesto
Serve un'app per Android, iOS e web, con il web come versione **completa** (non vetrina). Sviluppatore singolo con esperienza KMP.

## Decisione
Kotlin Multiplatform + Compose Multiplatform con UI condivisa al 100%, target `android`, `iosArm64`, `iosSimulatorArm64`, `wasmJs`. Un solo modulo `composeApp`, progetto generato dal wizard KMP.

## Alternative scartate
- Web con Kotlin/JS + UI HTML separata: migliore SEO/accessibilità, ma UI da scrivere due volte. Il SEO non serve (app privata dietro login).

## Conseguenze
- Compose for Web (Wasm) richiede browser moderni; accettabile.
- Ogni libreria scelta deve supportare `wasmJs` (vincola backend e persistenza → 0002, 0004).
