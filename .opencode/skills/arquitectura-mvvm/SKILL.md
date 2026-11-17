---
name: arquitectura-mvvm
description: Patrón MVVM en JavaFX para gestormrpet (ViewModels observables, bindings, navegación, inyección y threading). Úsala al crear pantallas, controladores o ViewModels.
---

# MVVM en gestormrpet

## Capas
`View → ViewModel → Service → Repository → Domain`

- **Domain**: entidades y reglas. Sin `javafx.*`.
- **Service**: casos de uso y validación.
- **Repository**: JDBC/SQLite.
- **ViewModel**: estado y comandos.
- **View**: FXML + controller delgado.

## ViewModel
- Expón estado con `StringProperty`, `BooleanProperty`, `ObjectProperty`, `ObservableList`.
- Comandos como métodos (`guardar()`, `buscar()`, `cargar()`), con `canX` (`BooleanProperty`) para habilitar botones.
- Validación en el VM; expón `errorMessage`/`valido`.
- Orquesta servicios; **no** abre ventanas ni toca `Stage`/`Node`.
- Sin `Platform.runLater` salvo integración con servicios asíncronos; debe testearse sin toolkit.

## View / Controller
- El controller solo enlaza: `textField.textProperty().bindBidirectional(vm.nombreProperty())`.
- Inyección por constructor vía `FXMLLoader#setControllerFactory`.
- Nada de SQL, cálculos ni reglas de negocio en el controller.
- Botones: `disableProperty().bind(vm.puedeGuardarProperty().not())`.

## Navegación
- `NavigationService.show(String vista, Object viewModel)` sobre un `StackPane` del `MainTemplate`.
- No navegar desde el ViewModel; devolver eventos/estado y que la View orqueste.

## Threading
- Operaciones de BD rápidas en el hilo FX; si se prevé lentitud, usar `javafx.concurrent.Task` y actualizar propiedades en `setOnSucceeded`.
- Nunca bloquear el hilo de UI con esperas.

## Pruebas
- Testea ViewModels sin JavaFX toolkit (propiedades y listas funcionan).
- Cubre validaciones y transiciones de estado.
