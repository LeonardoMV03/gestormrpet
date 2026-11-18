package net.martinezoft.domain;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Visita de una mascota. Las transiciones de estado y las reglas de cobro
 * viven aquí; la vista solo muestra los mensajes resultantes.
 */
public record Cita(
        Long id,
        Long mascotaId,
        LocalDateTime fechaHora,
        EstadoCita estado,
        String notas,
        EstadoPago estadoPago,
        MetodoPago metodoPago,
        Instant pagadoEn) {

    public Cita {
        if (mascotaId == null) {
            throw new IllegalArgumentException("La visita debe pertenecer a una mascota");
        }
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora de la visita son obligatorias");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la visita es obligatorio");
        }
        if (estadoPago == null) {
            throw new IllegalArgumentException("El estado de pago es obligatorio");
        }
        notas = notas == null || notas.isBlank() ? null : notas.trim();
        if (estadoPago == EstadoPago.PAGADO) {
            if (metodoPago == null || pagadoEn == null) {
                throw new IllegalArgumentException("Una visita pagada exige método y fecha de pago");
            }
        } else if (metodoPago != null || pagadoEn != null) {
            throw new IllegalArgumentException("Una visita a deber no puede tener método ni fecha de pago");
        }
    }

    public static Cita nueva(Long mascotaId, LocalDateTime fechaHora, String notas) {
        return new Cita(null, mascotaId, fechaHora, EstadoCita.PENDIENTE, notas,
                EstadoPago.A_DEBER, null, null);
    }

    public Cita atender() {
        if (estado != EstadoCita.PENDIENTE) {
            throw new IllegalArgumentException("Solo una visita pendiente puede marcarse como atendida");
        }
        return conEstado(EstadoCita.ATENDIDA);
    }

    public Cita cancelar() {
        if (estado != EstadoCita.PENDIENTE) {
            throw new IllegalArgumentException("Solo una visita pendiente puede cancelarse");
        }
        return conEstado(EstadoCita.CANCELADA);
    }

    public Cita registrarPago(MetodoPago metodo, Instant momento) {
        if (estado != EstadoCita.ATENDIDA) {
            throw new IllegalArgumentException("Solo se puede cobrar una visita atendida");
        }
        if (estadoPago == EstadoPago.PAGADO) {
            throw new IllegalArgumentException("La visita ya está pagada");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        if (momento == null) {
            throw new IllegalArgumentException("La fecha de pago es obligatoria");
        }
        return new Cita(id, mascotaId, fechaHora, estado, notas,
                EstadoPago.PAGADO, metodo, momento);
    }

    public boolean estaPendiente() {
        return estado == EstadoCita.PENDIENTE;
    }

    public boolean estaAtendida() {
        return estado == EstadoCita.ATENDIDA;
    }

    public boolean estaPagada() {
        return estadoPago == EstadoPago.PAGADO;
    }

    private Cita conEstado(EstadoCita nuevoEstado) {
        return new Cita(id, mascotaId, fechaHora, nuevoEstado, notas, estadoPago, metodoPago, pagadoEn);
    }
}
