package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class CitaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 18, 10, 0);
    private static final Instant PAGO = Instant.parse("2026-11-18T15:00:00Z");

    @Test
    void debeCrearCitaNuevaPendienteYADeber() {
        Cita cita = Cita.nueva(3L, FECHA, "  Primera visita  ");

        assertNull(cita.id());
        assertEquals(EstadoCita.PENDIENTE, cita.estado());
        assertEquals(EstadoPago.A_DEBER, cita.estadoPago());
        assertNull(cita.metodoPago());
        assertNull(cita.pagadoEn());
        assertEquals("Primera visita", cita.notas());
        assertTrue(cita.estaPendiente());
    }

    @Test
    void debeAtenderUnaCitaPendiente() {
        Cita atendida = Cita.nueva(3L, FECHA, null).atender();

        assertEquals(EstadoCita.ATENDIDA, atendida.estado());
        assertTrue(atendida.estaAtendida());
        assertFalse(atendida.estaPendiente());
    }

    @Test
    void debeCancelarUnaCitaPendiente() {
        Cita cancelada = Cita.nueva(3L, FECHA, null).cancelar();

        assertEquals(EstadoCita.CANCELADA, cancelada.estado());
        assertFalse(cancelada.estaPendiente());
    }

    @Test
    void debeRechazarAtenderUnaCitaYaAtendida() {
        Cita atendida = Cita.nueva(3L, FECHA, null).atender();

        assertThrows(IllegalArgumentException.class, atendida::atender);
    }

    @Test
    void debeRechazarCancelarUnaCitaYaCancelada() {
        Cita cancelada = Cita.nueva(3L, FECHA, null).cancelar();

        assertThrows(IllegalArgumentException.class, cancelada::cancelar);
    }

    @Test
    void debeRegistrarPagoEnUnaCitaAtendida() {
        Cita pagada = Cita.nueva(3L, FECHA, null)
                .atender()
                .registrarPago(MetodoPago.EFECTIVO, PAGO);

        assertEquals(EstadoPago.PAGADO, pagada.estadoPago());
        assertEquals(MetodoPago.EFECTIVO, pagada.metodoPago());
        assertEquals(PAGO, pagada.pagadoEn());
        assertTrue(pagada.estaPagada());
    }

    @Test
    void debeRechazarCobrarUnaCitaPendiente() {
        Cita pendiente = Cita.nueva(3L, FECHA, null);

        assertThrows(IllegalArgumentException.class,
                () -> pendiente.registrarPago(MetodoPago.EFECTIVO, PAGO));
    }

    @Test
    void debeRechazarPagarDosVecesLaMismaCita() {
        Cita pagada = Cita.nueva(3L, FECHA, null).atender()
                .registrarPago(MetodoPago.EFECTIVO, PAGO);

        assertThrows(IllegalArgumentException.class,
                () -> pagada.registrarPago(MetodoPago.TRANSFERENCIA, PAGO));
    }

    @Test
    void debeExigirMetodoYFechaCuandoLaCitaEstaPagada() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cita(1L, 3L, FECHA, EstadoCita.ATENDIDA, null,
                        EstadoPago.PAGADO, null, PAGO));
        assertThrows(IllegalArgumentException.class,
                () -> new Cita(1L, 3L, FECHA, EstadoCita.ATENDIDA, null,
                        EstadoPago.PAGADO, MetodoPago.EFECTIVO, null));
    }

    @Test
    void debeRechazarMetodoOFechaEnUnaCitaADeber() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cita(1L, 3L, FECHA, EstadoCita.ATENDIDA, null,
                        EstadoPago.A_DEBER, MetodoPago.EFECTIVO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Cita(1L, 3L, FECHA, EstadoCita.ATENDIDA, null,
                        EstadoPago.A_DEBER, null, PAGO));
    }

    @Test
    void debeExigirMascotaYFecha() {
        assertThrows(IllegalArgumentException.class, () -> Cita.nueva(null, FECHA, null));
        assertThrows(IllegalArgumentException.class, () -> Cita.nueva(3L, null, null));
    }

    @Test
    void debeCalcularElTotalDesdeLosPreciosCongelados() {
        CitaDetalle detalle = detalle(
                new CitaServicio(null, 1L, "Baño", 15050),
                new CitaServicio(null, 2L, "Corte", 20000));

        assertEquals(35050, detalle.totalCentavos());
    }

    @Test
    void debeRechazarUnaVisitaSinServicios() {
        Cita cita = Cita.nueva(3L, FECHA, null);

        assertThrows(IllegalArgumentException.class,
                () -> new CitaDetalle(cita, 1L, "Luna", "Ana", List.of()));
    }

    @Test
    void debeConservarLosServiciosAlActualizarLaCitaEnElDetalle() {
        CitaDetalle detalle = detalle(new CitaServicio(null, 1L, "Baño", 15050));

        CitaDetalle atendida = detalle.conCita(detalle.cita().atender());

        assertEquals(EstadoCita.ATENDIDA, atendida.cita().estado());
        assertEquals(detalle.servicios(), atendida.servicios());
        assertEquals(detalle.totalCentavos(), atendida.totalCentavos());
    }

    @Test
    void debeCongelarElPrecioYNombreDelServicioAlCrearElDetalle() {
        Servicio servicio = new Servicio(9L, "Baño", null, 15050, 45, true);

        CitaServicio congelado = CitaServicio.de(servicio);

        assertEquals(9L, congelado.servicioId());
        assertEquals("Baño", congelado.servicioNombre());
        assertEquals(15050, congelado.precioCentavos());
        assertNull(congelado.citaId());
    }

    @Test
    void debeRechazarServiciosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new CitaServicio(null, null, "Baño", 15050));
        assertThrows(IllegalArgumentException.class,
                () -> new CitaServicio(null, 1L, "  ", 15050));
        assertThrows(IllegalArgumentException.class,
                () -> new CitaServicio(null, 1L, "Baño", -1));
    }

    @Test
    void debeRechazarEstadosDesconocidos() {
        assertThrows(IllegalArgumentException.class, () -> EstadoCita.desde("confirmada"));
        assertThrows(IllegalArgumentException.class, () -> EstadoPago.desde("pendiente"));
        assertThrows(IllegalArgumentException.class, () -> MetodoPago.desde("tarjeta"));
    }

    private static CitaDetalle detalle(CitaServicio... servicios) {
        Cita cita = Cita.nueva(3L, FECHA, null);
        return new CitaDetalle(cita, 1L, "Luna", "Ana", List.of(servicios));
    }
}
