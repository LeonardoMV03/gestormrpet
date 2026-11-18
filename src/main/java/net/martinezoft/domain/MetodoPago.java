package net.martinezoft.domain;

import java.util.Arrays;

/**
 * Forma en que se cobró una visita.
 */
public enum MetodoPago {

    EFECTIVO("efectivo", "Efectivo"),
    TRANSFERENCIA("transferencia", "Transferencia");

    private final String valor;
    private final String etiqueta;

    MetodoPago(String valor, String etiqueta) {
        this.valor = valor;
        this.etiqueta = etiqueta;
    }

    public String valor() {
        return valor;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static MetodoPago desde(String valor) {
        return Arrays.stream(values())
                .filter(metodo -> metodo.valor.equals(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Método de pago desconocido: " + valor));
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
