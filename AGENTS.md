# AGENTS.md — gestormrpet

Contrato de trabajo para agentes de IA. Léelo completo antes de actuar.

## 1. Qué es gestormrpet

Aplicación de escritorio para gestionar una **estética canina** operada por una o pocas personas de confianza. Administra:

- **Clientes** (dueños).
- **Mascotas** de cada cliente.
- **Servicios** ofrecidos (precio y duración).
- **Citas** de mascotas con uno o más servicios.
- **Pagos** por cita: efectivo, transferencia o "a deber".
- **Historial** por mascota/cliente: servicios aplicados y notas.

Un solo usuario (`admin`), contraseña cambiable, sesión de 12 horas o hasta cerrar la app.

## 2. Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Java 25 (Temurin) |
| UI | JavaFX 27 + FXML + CSS |
| Build | Maven 3.10 |
| Base de datos | SQLite (`sqlite-jdbc`) + Flyway |
| Pruebas | JUnit 5 + TestFX |
| VCS | Git + GitHub (`gh` CLI) |

## 3. Estructura

```
src/main/java/net/martinezoft/
├── App.java
├── di/            AppContext (composition root)
├── config/        AppConfig, SesionManager
├── domain/        entidades y reglas (sin javafx)
├── data/          Database, repositorios JDBC
├── service/       ClienteService, CitaService, AuthService...
├── security/      PasswordHasher
└── ui/
    ├── design/      tokens.css, theme.css
    ├── atoms/       AppButton, AppTextField, Badge...
    ├── molecules/   FormField, SearchBar, ServiceCard...
    ├── organisms/   SidebarNav, ClientesTable, CitaForm...
    ├── templates/   LoginTemplate, MainTemplate
    ├── pages/       LoginPage, ClientesPage, CitasPage...
    ├── viewmodels/  *ViewModel
    ├── controllers/ delgados
    └── navigation/  NavigationService

src/main/resources/db/migration/   V1__..., V2__..., V3__...
```

## 4. Arquitectura MVVM

Dependencias: `View → ViewModel → Service → Repository → Domain`.

- **Model/Domain**: entidades, reglas y persistencia. **Prohibido** importar `javafx.*`.
- **ViewModel**: estado observable (`StringProperty`, `ObservableList`, `BooleanProperty`), validación, comandos (`guardar`, `buscar`) y orquestación. **No** toca `Stage`, `Node` ni FXML. Debe poder testearse sin toolkit JavaFX.
- **View**: FXML + CSS. El controller es delgado y **solo enlaza** (`bindBidirectional`) con el ViewModel.
- **Navigation**: `NavigationService.show(Page, ViewModel)`; no navegar desde dentro de un ViewModel.
- Inyección por constructor con `FXMLLoader#setControllerFactory`.
- **Prohibido** lógica de negocio, SQL o cálculos dentro de controllers/views.

## 5. Atomic Design

Niveles y composición (de menor a mayor):

1. **Atoms**: controles básicos sin lógica (`AppButton`, `AppTextField`, `Badge`).
2. **Molecules**: combinan átomos (`FormField`, `SearchBar`, `ServiceCard`, `PaymentSelector`).
3. **Organisms**: secciones funcionales (`SidebarNav`, `ClientesTable`, `CitaForm`).
4. **Templates**: layout con slots (`LoginTemplate`, `MainTemplate`).
5. **Pages**: vista completa unida a un ViewModel (`ClientesPage`, `CitasPage`).

Reglas:
- Cada componente = `.java` + `.fxml` (+ `.css` si aplica).
- Excepción: los **átomos sin layout** (`AppButton`, `AppTextField`, `AppPasswordField`) son clases Java puras que aplican su clase CSS en el constructor; no tienen `.fxml`.
- Componentes con `.fxml` usan `fx:root` (la instancia es la raíz y el controller) vía `ui/CargadorFxml`; la composición se hace en el constructor o en el FXML declarando el componente como elemento.
- Composición con `<fx:include>`; evitar duplicar markup.
- Nombres: `App*` (átomos), `*Field|*Card` (moléculas), `*Table|*Form|*Panel` (organismos), `*Template`, `*Page`.
- Estilos **solo** por clases CSS y tokens; **nunca** `setStyle()` inline.
- No crear un organismo si basta una molécula; no meter lógica en átomos.

## 6. Dominio y datos

Entidades: `Cliente`, `Mascota`, `Servicio`, `Cita`, `CitaServicio`, `NotaHistorial`, `Usuario`.

Reglas:
- Dinero en **centavos (INTEGER)**, nunca `float`.
- Fechas en **ISO-8601 TEXT**.
- **Soft delete** (`activo = 0`): nunca borrar historial físicamente.
- Precio congelado en `cita_servicios.precio_centavos` al crear la cita.
- Estados de cita: `pendiente → confirmada → atendida` o `cancelada`.
- Pago: `estado_pago` (`pagado|a_deber`) + `metodo_pago` (`efectivo|transferencia`, obligatorio si pagado).
- Historial = citas `atendida` + servicios aplicados + notas.
- `PRAGMA foreign_keys = ON`.

## 7. Migraciones (Flyway)

- Migraciones en `src/main/resources/db/migration/` con nombre `V<n>__descripcion.sql`.
- Flyway gestiona `flyway_schema_history` (versión, checksum, estado).
- **Nunca** editar una migración ya aplicada; todo cambio es una nueva `V<n>`.
- Se ejecutan al inicio, antes de abrir la UI.
- El usuario `admin` se crea de forma idempotente en código (`AuthService`), no en SQL.

## 8. Comandos

```
mvn clean compile          # compilar
mvn test                   # pruebas
mvn javafx:run             # ejecutar
mvn -B verify              # compilación + pruebas (CI)
```

## 9. Convenciones

- Paquetes en minúsculas; clases en `PascalCase`; métodos/variables en `camelCase`; constantes en `MAYÚSCULAS`.
- Español para nombres de dominio (`Cliente`, `Mascota`, `Cita`, `Historial`).
- Sin comentarios obvios; documentar solo decisiones no evidentes.
- No agregar dependencias sin justificarlo en el plan/PR.
- Código y textos en español; evitar `TODO` sin issue asociado.

## 10. Flujo multiagente por feature

Roles: **planificador** (plan corto) → **implementador** (código + tests) → **revisor** (solo en cambios de riesgo).

1. **Intake**: issue en GitHub con label `tipo:*` y `prioridad:*`. Criterios de aceptación en el cuerpo.
2. **Plan**: breve, en el issue. Para cambios de riesgo (auth, esquema, migraciones) el planificador pide aprobación antes de implementar.
3. **Implementación**: rama `feature/<slug>` desde `develop`; el implementador codea, escribe sus tests y commitea. Verifica con `mvn -B verify`.
4. **Revisión** (opcional, obligatoria en auth, esquema o migraciones): el revisor audita el diff vs criterios y AGENTS.md; emite `APROBADO`/`BLOQUEADO`.
5. **PR**: `gh pr create --base develop` con `Closes #<issue>`. Branch protection exige CI verde.
6. **Merge**: **lo hace el usuario** (squash, borrado de rama). La IA nunca mergea.

El estado de un issue es su estado en GitHub (abierto = pendiente, cerrado = hecho). No se usan labels `fase:*` ni Project.

## 11. Git

- Ramas: `main` (estable) ← `develop` (integración) ← `feature/*`, `fix/*`, `chore/*`, `test/*`, `docs/*`.
- Commits: Conventional Commits en español — `feat:`, `fix:`, `test:`, `chore:`, `docs:`, `refactor:`. Sujeto imperativo, ≤72 caracteres.
- La IA puede: crear ramas, `add`, `commit`, `push origin feature/*`, crear/comentar PRs.
- La IA **no** puede: push a `main`/`develop`, force-push, `reset --hard`, `clean`, mergear PRs.
- Merge con **squash** y borrado de rama.

## 12. Pruebas y Definition of Done

- JUnit 5 para dominio, servicios y repositorios (SQLite temporal por test).
- TestFX solo para smoke tests de UI.
- **DoD**: `mvn -B verify` verde (local y CI), tests de la lógica nueva, sin secretos, PR squash-mergeado por el usuario e issue cerrado. Revisor APROBADO solo si aplica el punto 4 del flujo.

## 13. Seguridad

- Nunca escribir ni loguear secretos, contraseñas o tokens.
- Contraseñas con **PBKDF2** (salt aleatorio + iteraciones); jamás texto plano.
- La BD vive en `%APPDATA%\gestormrpet\gestormrpet.db` (fuera de OneDrive).
- No versionar `*.db*`, respaldos ni `.env`.

## 14. Comunicación

- Responde en **español**, conciso y directo.
- Si falta información, **pregunta**; no inventes requisitos.
- Reporta bloqueos y riesgos de inmediato.
- Al terminar una tarea, indica qué cambió, cómo se verificó y el siguiente paso.

## 15. Estado y roadmap

- **Roadmap y contexto:** https://github.com/LeonardoMV03/gestormrpet/issues/21 (issue pineado).
- Antes de empezar una feature, revisa el roadmap y elige un issue abierto y no bloqueado. **El issue es la fuente de verdad del alcance.**
- Labels vigentes: `tipo:feature|bug|infra|docs`, `prioridad:alta|media|baja`, `bloqueado`.
- Protección de `main` y `develop`: PR obligatorio y check `Compilar y probar` requerido. Sin force-push ni borrado.
