---
name: pruebas-junit-testfx
description: Pruebas en gestormrpet con JUnit 5 y TestFX (BD SQLite temporal, ViewModels sin toolkit, smoke tests de UI). Úsala al escribir o ejecutar tests.
---

# Pruebas con JUnit 5 + TestFX

## Estructura
```
src/test/java/net/martinezoft/
├── domain/     reglas puras
├── service/    casos de uso
├── data/       repositorios + migraciones
└── ui/         ViewModels y smoke tests TestFX
```

## JUnit 5
- Nombres descriptivos en español: `debeRechazarCitaSinServicios`.
- Patrón given/when/then.
- Sin estado compartido entre tests.
- Aserciones claras (AssertJ opcional).

## BD temporal
- `@TempDir Path tmp` + `jdbc:sqlite:<tmp>/test.db`.
- Ejecuta Flyway en `@BeforeEach`; elimina archivo en `@AfterEach`.
- Nunca apuntes a la BD real del usuario.

## ViewModels
- Testea sin toolkit JavaFX: propiedades y `ObservableList` funcionan sin `Platform`.
- Verifica validaciones, `canX` y transiciones de estado.
- Mockea servicios con stubs simples (interfaces) o implementaciones en memoria.

## TestFX (smoke)
- Solo para flujos críticos (login, navegación básica).
- Headless: `-Dtestfx.headless=true -Dprism.order=sw`.
- Requiere `--add-opens` en surefire si hay módulos.
- Evita tests frágiles por timing; usa `WaitForAsyncUtils`.

## Comandos
```
mvn test
mvn -B verify
```
Ambos deben quedar verdes antes de cerrar una feature.
