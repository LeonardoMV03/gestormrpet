package net.martinezoft.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Mascota de un cliente. {@code clienteId} es obligatorio y la especie se
 * normaliza a minúsculas con {@code perro} por defecto. El borrado es lógico.
 */
public record Mascota(
        Long id,
        Long clienteId,
        String nombre,
        String especie,
        String raza,
        String sexo,
        String fechaNacimiento,
        String notas,
        boolean activo) {

    private static final Set<String> SEXOS = Set.of("macho", "hembra");

    public Mascota {
        if (clienteId == null) {
            throw new IllegalArgumentException("La mascota debe pertenecer a un cliente");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota es obligatorio");
        }
        nombre = nombre.trim();
        especie = especie == null || especie.isBlank() ? "perro" : especie.trim().toLowerCase();
        raza = normalizarOpcional(raza);
        sexo = normalizarSexo(sexo);
        fechaNacimiento = normalizarFecha(fechaNacimiento);
        notas = normalizarOpcional(notas);
    }

    public static Mascota nueva(Long clienteId, String nombre, String especie, String raza,
                                String sexo, String fechaNacimiento, String notas) {
        return new Mascota(null, clienteId, nombre, especie, raza, sexo, fechaNacimiento, notas, true);
    }

    private static String normalizarSexo(String sexo) {
        if (sexo == null || sexo.isBlank()) {
            return null;
        }
        String limpio = sexo.trim().toLowerCase();
        if (!SEXOS.contains(limpio)) {
            throw new IllegalArgumentException("El sexo debe ser macho o hembra");
        }
        return limpio;
    }

    private static String normalizarFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha de nacimiento debe tener formato AAAA-MM-DD", e);
        }
    }

    private static String normalizarOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
