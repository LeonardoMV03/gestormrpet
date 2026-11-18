package net.martinezoft.domain;

import java.util.Objects;

/**
 * Mascota con el nombre de su dueño, para selectores de visitas.
 */
public record MascotaConDueno(Mascota mascota, String clienteNombre) {

    public MascotaConDueno {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        if (clienteNombre == null || clienteNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        clienteNombre = clienteNombre.trim();
    }

    public String etiqueta() {
        return mascota.nombre() + " · " + clienteNombre;
    }
}
