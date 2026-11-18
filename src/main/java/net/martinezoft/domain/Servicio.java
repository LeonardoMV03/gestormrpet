package net.martinezoft.domain;

/**
 * Prestación ofrecida por la estética. El precio se guarda en centavos y la
 * duración es opcional. El borrado es lógico: {@code activo=false}.
 */
public record Servicio(
        Long id,
        String nombre,
        String descripcion,
        long precioCentavos,
        Integer duracionMin,
        boolean activo) {

    public Servicio {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        nombre = nombre.trim();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (precioCentavos < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (duracionMin != null && duracionMin < 0) {
            throw new IllegalArgumentException("La duración no puede ser negativa");
        }
    }

    public static Servicio nuevo(String nombre, String descripcion, long precioCentavos, Integer duracionMin) {
        return new Servicio(null, nombre, descripcion, precioCentavos, duracionMin, true);
    }
}
