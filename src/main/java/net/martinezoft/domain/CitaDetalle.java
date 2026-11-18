package net.martinezoft.domain;

import java.util.List;
import java.util.Objects;

/**
 * Visita con los datos necesarios para mostrarla: dueño, mascota y servicios
 * aplicados. El total siempre se calcula desde los precios congelados.
 */
public record CitaDetalle(
        Cita cita,
        Long clienteId,
        String mascotaNombre,
        String clienteNombre,
        List<CitaServicio> servicios) {

    public CitaDetalle {
        Objects.requireNonNull(cita, "La cita es obligatoria");
        if (clienteId == null) {
            throw new IllegalArgumentException("El cliente de la visita es obligatorio");
        }
        if (mascotaNombre == null || mascotaNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota es obligatorio");
        }
        if (clienteNombre == null || clienteNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (servicios == null || servicios.isEmpty()) {
            throw new IllegalArgumentException("La visita debe tener al menos un servicio");
        }
        servicios = List.copyOf(servicios);
    }

    public long totalCentavos() {
        return servicios.stream().mapToLong(CitaServicio::precioCentavos).sum();
    }

    public CitaDetalle conCita(Cita nueva) {
        return new CitaDetalle(nueva, clienteId, mascotaNombre, clienteNombre, servicios);
    }
}
