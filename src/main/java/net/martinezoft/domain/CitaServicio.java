package net.martinezoft.domain;

/**
 * Servicio aplicado en una visita. El precio queda congelado al crear la cita y
 * no cambia aunque el catálogo se actualice después.
 */
public record CitaServicio(
        Long citaId,
        Long servicioId,
        String servicioNombre,
        long precioCentavos) {

    public CitaServicio {
        if (servicioId == null) {
            throw new IllegalArgumentException("El servicio es obligatorio");
        }
        if (servicioNombre == null || servicioNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        servicioNombre = servicioNombre.trim();
        if (precioCentavos < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }

    public static CitaServicio de(Servicio servicio) {
        return new CitaServicio(null, servicio.id(), servicio.nombre(), servicio.precioCentavos());
    }

    public CitaServicio conCitaId(long id) {
        return new CitaServicio(id, servicioId, servicioNombre, precioCentavos);
    }
}
