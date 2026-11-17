---
description: Ejecuta el ciclo completo por feature (issue, plan, implementación, test, review, PR).
agent: planificador
---

Orquestas el flujo por feature de gestormrpet. Sigue `AGENTS.md` y las skills del proyecto.

Solicitud: $ARGUMENTS

## Preflight
- `git status` limpio y rama `develop` disponible.
- `gh auth status` válido; si falla, detente y pide autenticación.

## Pasos
1. **Intake** — Crea el issue:
   `gh issue create --title "[Feature] $ARGUMENTS" --label "tipo:feature,fase:plan" --body "<contexto y criterios>"`
   Anota el número de issue y asígnalo al milestone correspondiente.
2. **Plan** — Genera el plan en `.opencode/plans/<slug>.md` y publícalo con `gh issue comment <n> --body-file .opencode/plans/<slug>.md`.
3. **Gate** — Pide aprobación del plan con `question`. No continúes sin aprobación explícita. Si rechaza, ajusta y repite.
4. **Rama** — `git switch -c feature/<slug> develop`.
5. **Implementación** — Invoca al subagente `implementador` con `task`, pasándole el plan y la rama.
6. **Test** — Invoca al subagente `tester`; exige `mvn test` en verde.
7. **Review** — Invoca al subagente `revisor`; debe emitir `APROBADO`.
8. **PR** — `git push -u origin feature/<slug>` y `gh pr create --base develop --title "feat: ..." --body "Closes #<n>"`.
9. **CI** — `gh pr checks --watch`; si falla, vuelve al implementador.
10. **Merge** — **No mergees**. Pide al usuario el squash-merge manual y el borrado de rama.
11. **Cierre** — Actualiza label a `fase:listo`, comenta el resultado y mueve la tarjeta del Project.

## Reglas
- Un gate por fase; reporta estado al usuario en cada uno.
- Si algo falla o es ambiguo, detente y pregunta.
- Nunca mergees, nunca hagas push a `main`/`develop`.
