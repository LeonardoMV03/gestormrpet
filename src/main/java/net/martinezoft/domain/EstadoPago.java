package net.martinezoft.domain;

import java.util.Arrays;

/**
 * Situación de cobro de una visita.
 */
public enum EstadoPago {

    PAGADO("pagado", "Pagado"),
    A_DEBER("a_deber", "A deber");

    private final String valor;
    private final String etiqueta;

    EstadoPago(String valor, String etiqueta) {
        this.valor = valor;
        this.etiqueta = etiqueta;
    }

    public String valor() {
        return valor;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static EstadoPago desde(String valor) {
        return Arrays.stream(values())
                .filter(estado -> estado.valor.equals(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de pago desconocido: " + valor));
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
