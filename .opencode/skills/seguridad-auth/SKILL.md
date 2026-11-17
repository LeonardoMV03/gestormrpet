---
name: seguridad-auth
description: Autenticación de usuario único en gestormrpet (PBKDF2, sesión de 12 h, cambio de contraseña). Úsala al implementar login, sesión o manejo de contraseñas.
---

# Seguridad y autenticación

## Usuario
- Un solo usuario: `admin`, creado idempotentemente por `AuthService` si no existe.
- Contraseña inicial `admin` (hash generado en runtime, **nunca** texto plano en SQL).
- Aviso persistente para cambiar la contraseña por defecto (no forzar).

## Hash de contraseñas
- **PBKDF2WithHmacSHA256** (JDK): salt aleatorio de 16 bytes, ≥ 210000 iteraciones.
- Formato almacenado: `pbkdf2$sha256$<iteraciones>$<saltBase64>$<hashBase64>`.
- Verifica con comparación en tiempo constante (`MessageDigest.isEqual`).
- Nunca loguees contraseñas ni hashes.

## Sesión
- `SesionManager` en memoria: `usuario`, `iniciaEn`, `expiraEn = inicio + 12 h`.
- Al cerrar la app la sesión se pierde (no persistir tokens).
- Un `Timeline` (o chequeo al navegar) expira la sesión y vuelve al login.
- `Clock` inyectable para testear expiración.

## Flujo
1. Login valida usuario/contraseña.
2. Si es la contraseña por defecto, mostrar aviso de cambio.
3. La ventana principal solo se abre con sesión válida.
4. Ajustes permite cambiar contraseña (pedir actual + nueva + confirmación).
5. Al expirar: cerrar sesión y mostrar login.

## Validaciones
- Nueva contraseña: mínimo 8 caracteres, no igual a la anterior, confirmación coincidente.
- Rate limiting simple opcional (retardo tras N intentos fallidos).

## Tests
- Hash/verify correcto e incorrecto.
- Seed idempotente (no duplica admin).
- Expiración con `Clock` fijo.
- Cambio de contraseña y login con la nueva.
