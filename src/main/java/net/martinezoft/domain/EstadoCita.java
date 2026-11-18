package net.martinezoft.domain;

import java.util.Arrays;

/**
 * Estado de una visita. Solo una visita pendiente puede atenderse o cancelarse.
 */
public enum EstadoCita {

    PENDIENTE("pendiente", "Pendiente"),
    ATENDIDA("atendida", "Atendida"),
    CANCELADA("cancelada", "Cancelada");

    private final String valor;
    private final String etiqueta;

    EstadoCita(String valor, String etiqueta) {
        this.valor = valor;
        this.etiqueta = etiqueta;
    }

    public String valor() {
        return valor;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static EstadoCita desde(String valor) {
        return Arrays.stream(values())
                .filter(estado -> estado.valor.equals(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de visita desconocido: " + valor));
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
