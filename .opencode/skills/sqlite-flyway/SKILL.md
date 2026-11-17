---
name: sqlite-flyway
description: SQLite con Flyway en gestormrpet (migraciones versionadas, PRAGMAs, repositorios JDBC y pruebas con BD temporal). Úsala al tocar esquema, migraciones o acceso a datos.
---

# SQLite + Flyway

## Conexión
- Driver `org.xerial:sqlite-jdbc`, URL `jdbc:sqlite:<ruta>`.
- BD en `%APPDATA%\gestormrpet\gestormrpet.db`; override con `-Dgestormrpet.db=...`.
- Al abrir conexión:
```sql
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;
```

## Migraciones
- Ubicación: `src/main/resources/db/migration/`.
- Nombre: `V<n>__descripcion.sql` (`V1__esquema_base.sql`, `V2__pagos_citas.sql`).
- Flyway crea `flyway_schema_history`; **no** la modifiques a mano.
- **Nunca** edites una migración aplicada: crea la siguiente versión.
- Ejecuta `migrate()` al inicio, antes de la UI.
- No uses `CREATE TRANSACTION`/`COMMIT` dentro de migraciones (SQLite no anida transacciones).
- Datos semilla críticos: en código idempotente, no en SQL (ej. usuario admin con PBKDF2).

## Repositorios
- Un repositorio por agregado (`ClienteRepository`, `MascotaRepository`...).
- SQL con `PreparedStatement`; nunca concatenar valores del usuario.
- Mapea `ResultSet` a entidades de dominio.
- Transacciones explícitas para operaciones multi-tabla (crear cita + servicios).
- Dinero: `INTEGER` centavos. Fechas: `TEXT` ISO-8601.

## Pruebas
- BD temporal por test: `Files.createTempFile("gestormrpet", ".db")` + `@TempDir`.
- Migra con Flyway en `@BeforeEach`; borra al terminar.
- Prueba migraciones y repositorios por separado.
