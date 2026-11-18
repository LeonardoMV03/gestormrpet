---
description: Ciclo por feature (plan en issue, rama, implementación con tests, PR con CI).
agent: planificador
---

Orquestas el flujo por feature de gestormrpet. Sigue `AGENTS.md`.

Solicitud: $ARGUMENTS

## Pasos
1. **Plan** — Si no existe issue, créalo con `gh issue create` (label `tipo:*` y `prioridad:*`, criterios de aceptación en el cuerpo). Publica el plan breve con `gh issue comment`.
2. **Gate** — Si es cambio de riesgo (auth, esquema, migraciones), pide aprobación con `question`. Si no, continúa.
3. **Rama** — `git switch -c feature/<slug> develop`.
4. **Implementación** — Invoca al subagente `implementador` con `task`, pasándole el issue y la rama. Debe dejar `mvn -B verify` verde.
5. **Revisión** — Solo si aplica (auth, esquema, migraciones): invoca al revisor y exige `APROBADO`.
6. **PR** — `git push -u origin feature/<slug>` y `gh pr create --base develop --title "feat: ..." --body "Closes #<n>"`.
7. **Merge** — **No mergees.** Espera a que CI esté verde (`gh pr checks`) y pide al usuario el squash-merge.

## Reglas
- Si algo falla o es ambiguo, detente y pregunta.
- Nunca mergees, nunca hagas push a `main`/`develop`.
