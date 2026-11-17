---
description: Muestra el estado de features, PRs, CI y tablero del proyecto.
agent: planificador
---

Genera un tablero de estado y resúmelo en una tabla.

Proyecto (opcional): $ARGUMENTS

Ejecuta y resume:
- `gh issue list --state open --json number,title,labels,milestone --limit 50`
- `gh pr list --state open --json number,title,reviewDecision,statusCheckRollup --limit 50`
- `gh run list --limit 10`
- Si se indicó número de Project: `gh project item-list <n> --owner LeonardoMV03 --format json`

Devuelve:
1. Features abiertas por fase (`fase:plan|impl|test|review|listo|bloqueado`).
2. PRs abiertos con estado de revisión y CI.
3. Últimos runs de CI (verde/rojo).
4. Bloqueos y siguiente acción recomendada.
