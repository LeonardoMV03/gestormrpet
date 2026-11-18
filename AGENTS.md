# AGENTS.md — gestormrpet

Contrato de trabajo. Léelo antes de actuar.

## Qué es

App de **escritorio** (JavaFX) para que el dueño de una estética canina —solo o con pocas personas de confianza— gestione clientes, mascotas, servicios, visitas, cobros e historial. Reemplaza la libreta y WhatsApp. Un usuario `admin`, contraseña cambiable, sesión de 12 h.

## Stack

Java 25 · JavaFX 27 (FXML + CSS) · Maven · SQLite + Flyway · JUnit 5 · Git/GitHub (`gh`).

## Estructura

```
src/main/java/net/martinezoft/
├── App.java
├── di/        AppContext (composition root)
├── config/    AppConfig, SesionManager
├── domain/    entidades y reglas (sin javafx)
├── data/      Database, migraciones y repositorios JDBC
├── service/   AuthService, ServicioService...
├── security/  PasswordHasher
└── ui/
    ├── viewmodels/  estado observable y comandos
    ├── components/  formularios, tablas y widgets reutilizables
    ├── pages/       pantallas completas
    ├── templates/   layouts con slots
    ├── navigation/  NavigationService
    └── design/      tokens.css, theme.css
```

## Arquitectura

`View → ViewModel → Service → Repository → Domain`.

- **Domain**: reglas y entidades; prohibido importar `javafx`.
- **ViewModel**: propiedades observables, validación de entrada y comandos. No toca `Stage`, `Node` ni FXML; debe poder testearse sin toolkit.
- **View/controller**: solo enlaza con el ViewModel. Prohibido SQL, cálculos o reglas de negocio.
- Las **reglas de dominio se validan en una sola fuente**: la entidad de `domain/`. La vista solo muestra el mensaje.
- Estilos solo por clases CSS y tokens de `design/tokens.css`; nunca `setStyle()` inline.

## Dominio y datos

- Dinero en **centavos (INTEGER)**, nunca `float`.
- Fechas ISO-8601 TEXT.
- **Soft delete** (`activo = 0`); nunca borrar historial.
- Precio congelado en `cita_servicios.precio_centavos` al crear la visita.
- Visitas: `pendiente → atendida` o `cancelada`.
- Pago: `estado_pago` (`pagado|a_deber`) + `metodo_pago` (`efectivo|transferencia`, obligatorio si pagado).
- `PRAGMA foreign_keys = ON`.
- BD en `%APPDATA%\gestormrpet\gestormrpet.db`; no versionar `*.db*` ni `.env`.

## Migraciones

`src/main/resources/db/migration/V<n>__descripcion.sql`. **Nunca** editar una migración aplicada: todo cambio es una `V<n>` nueva. Se ejecutan al inicio. El usuario `admin` se crea en código (`AuthService`).

## Comandos

```
mvn -B verify     # compilar + probar (lo que exige el CI)
mvn test          # solo pruebas
mvn javafx:run    # ejecutar
```

## Convenciones

- Español para dominio y textos. Paquetes minúsculas, clases `PascalCase`, métodos `camelCase`.
- Sin comentarios obvios ni `TODO` sin issue. No agregar dependencias sin justificarlo.
- Commits: Conventional Commits en español, sujeto ≤72 caracteres.

## Flujo

- Ramas: `main` ← `develop` ← `feature/*`, `fix/*`, `chore/*`.
- Un issue = una rama = un PR a `develop`. Merge **squash** manual por el usuario.
- `main` y `develop` están protegidas: PR obligatorio y check `Compilar y probar` en verde. Sin force-push ni borrado.
- La IA no mergea ni hace push a `main`/`develop`.
- Plan breve en el issue. Aprobación explícita obligatoria en auth, esquema o migraciones.

## Seguimiento

- Roadmap: https://github.com/LeonardoMV03/gestormrpet/issues/21
- Project #5: https://github.com/users/LeonardoMV03/projects/5
- El estado vive en el campo **Fase** (`Backlog → Plan → Implementación → Test → Review → Listo`). La prioridad, en labels `prioridad:*`. Actualiza ambos al cambiar de estado.
