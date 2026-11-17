---
description: Revisa el diff actual contra el plan y AGENTS.md y emite veredicto.
agent: revisor
---

Revisa la rama actual contra el plan y `AGENTS.md`.

1. Identifica la rama y el issue/PR (`git status`, `git log --oneline develop..HEAD`).
2. Lee el plan en `.opencode/plans/<slug>.md` y el diff (`git diff develop...HEAD`).
3. Verifica: MVVM, Atomic Design, convenciones, migraciones Flyway, seguridad, tests y CI (`gh pr checks`).
4. Emite veredicto `APROBADO` o `BLOQUEADO` con hallazgos (bloqueante/importante/sugerencia) y archivo/línea.
5. Publica el veredicto con `gh pr comment` si existe PR.
