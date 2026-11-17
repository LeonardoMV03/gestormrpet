---
name: dominio-estetica-canina
description: Modelo y reglas de negocio de la estética canina (clientes, mascotas, servicios, citas, historial y pagos). Úsala al diseñar entidades, esquema SQLite o validaciones de dominio.
---

# Dominio — estética canina

## Glosario
- **Cliente**: dueño de una o más mascotas.
- **Mascota**: perro atendido; pertenece a un cliente.
- **Servicio**: prestación con precio y duración (baño, corte, etc.).
- **Cita**: encuentro de una mascota con uno o más servicios en fecha/hora.
- **CitaServicio**: relación cita–servicio con el precio congelado.
- **NotaHistorial**: observación sobre la mascota, opcionalmente ligada a una cita.
- **Usuario**: operador único (`admin`).

## Relaciones
```
clientes 1—N mascotas 1—N citas N—M servicios (cita_servicios)
mascotas 1—N notas_historial
```

## Reglas
- Un cliente puede tener varias mascotas; una mascota pertenece a un solo cliente.
- Una cita pertenece a una mascota e incluye **1..N** servicios.
- El precio se congela en `cita_servicios.precio_centavos` al crear la cita.
- Dinero en **centavos (INTEGER)**; fechas en **ISO-8601 TEXT**.
- **Soft delete** (`activo = 0`); nunca borrar historial físicamente.
- Estados de cita: `pendiente → confirmada → atendida` o `cancelada`.
- Pago por cita: `estado_pago` (`pagado|a_deber`) + `metodo_pago` (`efectivo|transferencia`).
- `pagado` exige `metodo_pago`; `a_deber` lo deja nulo.
- Total de la cita = `SUM(cita_servicios.precio_centavos)`.
- Historial = citas `atendida` + servicios aplicados + notas, orden cronológico.

## Validaciones típicas
- Nombre de cliente/mascota no vacío.
- Precio ≥ 0; duración ≥ 0.
- Fecha de cita futura al agendar (permitir reprogramar).
- No solapar citas de la misma mascota.
- No cancelar una cita `atendida`.
