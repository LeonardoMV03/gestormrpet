---
name: atomic-design-javafx
description: Atomic Design aplicado a JavaFX/FXML (átomos, moléculas, organismos, templates y páginas) con tokens CSS. Úsala al crear o componer componentes de UI.
---

# Atomic Design en JavaFX

## Niveles
1. **Atoms**: control básico, sin lógica. `AppButton`, `AppTextField`, `AppLabel`, `Badge`, `IconButton`.
2. **Molecules**: combinan átomos. `FormField` (label + input + error), `SearchBar`, `ServiceCard`, `PaymentSelector`.
3. **Organisms**: secciones funcionales. `SidebarNav`, `ClientesTable`, `MascotaForm`, `CitaForm`, `HistorialTimeline`.
4. **Templates**: layout con slots. `LoginTemplate`, `MainTemplate`.
5. **Pages**: composición completa + ViewModel. `LoginPage`, `ClientesPage`, `CitasPage`...

## Estructura
```
ui/design/        tokens.css, theme.css
ui/atoms/         <Nombre>.java + .fxml
ui/molecules/
ui/organisms/
ui/templates/
ui/pages/
ui/viewmodels/
ui/controllers/
ui/navigation/
```

## Naming
- Átomos: prefijo `App` (`AppButton`, `AppTextField`).
- Moléculas: `*Field`, `*Card`, `*Bar`, `*Selector`.
- Organismos: `*Table`, `*Form`, `*Panel`, `*Nav`.
- Templates: `*Template`. Páginas: `*Page`.

## Reglas
- Cada componente = `.java` + `.fxml` (+ `.css` si aplica).
- Composición con `<fx:include>`; no dupliques markup.
- No crear organismo si basta una molécula; no meter lógica en átomos.
- Estilos solo con clases CSS y tokens; **nunca** `setStyle()` inline.
- Un componente no conoce servicios ni base de datos; recibe datos por propiedades/VM.

## Tokens CSS
Define colores/tamaños en `tokens.css` sobre `.root` (looked-up colors de JavaFX):
```css
.root {
  -color-primary: #2563eb;
  -color-bg: #f8fafc;
  -color-surface: #ffffff;
  -color-text: #0f172a;
  -color-danger: #dc2626;
  -color-success: #16a34a;
  -radius-md: 8px;
}
```
Usa `-color-primary` en las clases de componentes; no repitas valores hex.
