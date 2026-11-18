package net.martinezoft.domain;

/**
 * Dueño de una o más mascotas. {@code id} es nulo hasta que el repositorio lo
 * persiste. El borrado es lógico: {@code activo=false}.
 */
public record Cliente(
        Long id,
        String nombre,
        String telefono,
        String email,
        String direccion,
        String notas,
        boolean activo) {

    public Cliente {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        nombre = nombre.trim();
        telefono = normalizarOpcional(telefono);
        email = normalizarOpcional(email);
        direccion = normalizarOpcional(direccion);
        notas = normalizarOpcional(notas);
    }

    public static Cliente nuevo(String nombre, String telefono, String email, String direccion, String notas) {
        return new Cliente(null, nombre, telefono, email, direccion, notas, true);
    }

    private static String normalizarOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
