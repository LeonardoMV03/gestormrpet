---
description: Genera el plan de una feature y lo publica en un issue, sin implementar.
agent: planificador
---

Genera el plan de la siguiente feature sin implementar nada.

Solicitud: $ARGUMENTS

1. Crea el issue con `gh issue create` (label `tipo:feature,fase:plan`) y anota el número.
2. Analiza el código actual y las skills aplicables.
3. Escribe el plan en `.opencode/plans/<slug>.md` con: objetivo, criterios de aceptación, tareas por capa, archivos afectados, riesgos y cómo se probará.
4. Publícalo con `gh issue comment <n> --body-file .opencode/plans/<slug>.md`.
5. Pide aprobación con `question` y detente.
