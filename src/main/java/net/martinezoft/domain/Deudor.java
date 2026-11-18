package net.martinezoft.domain;

/**
 * Total adeudado por un cliente con visitas atendidas sin pagar.
 */
public record Deudor(Long clienteId, String clienteNombre, long totalCentavos, int visitas) {

    public Deudor {
        if (clienteId == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (clienteNombre == null || clienteNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        clienteNombre = clienteNombre.trim();
        if (totalCentavos < 0) {
            throw new IllegalArgumentException("El total adeudado no puede ser negativo");
        }
        if (visitas < 0) {
            throw new IllegalArgumentException("La cantidad de visitas no puede ser negativa");
        }
    }
}
