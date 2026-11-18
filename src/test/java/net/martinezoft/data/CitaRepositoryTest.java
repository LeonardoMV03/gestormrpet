package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Cita;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.CitaServicio;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.EstadoCita;
import net.martinezoft.domain.Mascota;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.domain.Servicio;

class CitaRepositoryTest {

    @TempDir
    Path tempDir;

    private Database database;
    private CitaRepository citas;
    private Cliente cliente;
    private Mascota mascota;
    private Servicio bano;
    private Servicio corte;

    @BeforeEach
    void prepararRepositorio() throws SQLException {
        database = new Database(tempDir.resolve("citas.db"));
        new DatabaseInitializer(database).migrar();
        ClienteRepository clientes = new ClienteRepository(database);
        MascotaRepository mascotas = new MascotaRepository(database);
        ServicioRepository servicios = new ServicioRepository(database);
        citas = new CitaRepository(database);

        cliente = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null));
        mascota = mascotas.insertar(Mascota.nueva(cliente.id(), "Luna", null, null, null, null, null));
        bano = servicios.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        corte = servicios.insertar(Servicio.nuevo("Corte", null, 20000, 60));
    }

    @Test
    void debeInsertarYLeerLaVisitaConSusServicios() throws SQLException {
        CitaDetalle guardada = agendar(LocalDateTime.of(2026, 11, 18, 10, 0), bano, corte);

        assertEquals(mascota.id(), guardada.cita().mascotaId());
        assertEquals(cliente.id(), guardada.clienteId());
        assertEquals("Luna", guardada.mascotaNombre());
        assertEquals("Ana", guardada.clienteNombre());
        assertEquals(2, guardada.servicios().size());
        assertEquals(35050, guardada.totalCentavos());

        CitaDetalle leida = citas.buscarPorId(guardada.cita().id()).orElseThrow();
        assertEquals(guardada.cita().id(), leida.cita().id());
        assertEquals(EstadoCita.PENDIENTE, leida.cita().estado());
        assertEquals(2, leida.servicios().size());
    }

    @Test
    void debeCongelarElPrecioAunqueCambieElCatalogo() throws SQLException {
        CitaDetalle guardada = agendar(LocalDateTime.of(2026, 11, 18, 10, 0), bano);

        ServicioRepository servicios = new ServicioRepository(database);
        servicios.actualizar(new Servicio(bano.id(), "Baño", null, 25000, 45, true));

        CitaDetalle leida = citas.buscarPorId(guardada.cita().id()).orElseThrow();
        assertEquals(15050, leida.servicios().get(0).precioCentavos());
        assertEquals(15050, leida.totalCentavos());
    }

    @Test
    void debeListarEntreYDesdeUnaFecha() throws SQLException {
        LocalDate hoy = LocalDate.of(2026, 11, 18);
        CitaDetalle manana = agendar(hoy.plusDays(1).atTime(9, 0), bano);
        agendar(hoy.atTime(12, 0), corte);
        agendar(hoy.atTime(10, 0), bano);

        List<CitaDetalle> delDia = citas.listarEntre(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        List<CitaDetalle> proximas = citas.listarDesde(hoy.plusDays(1).atStartOfDay());

        assertEquals(2, delDia.size());
        assertEquals(10, delDia.get(0).cita().fechaHora().getHour());
        assertEquals(12, delDia.get(1).cita().fechaHora().getHour());
        assertEquals(List.of(manana.cita().id()), proximas.stream().map(cita -> cita.cita().id()).toList());
    }

    @Test
    void debeListarSoloAtendidasSinPagar() throws SQLException {
        CitaDetalle pendiente = agendar(LocalDateTime.of(2026, 11, 18, 9, 0), bano);
        CitaDetalle aDeber = agendar(LocalDateTime.of(2026, 11, 18, 10, 0), bano);
        CitaDetalle pagada = agendar(LocalDateTime.of(2026, 11, 18, 11, 0), bano);

        citas.actualizarEstado(aDeber.cita().id(), EstadoCita.ATENDIDA);
        citas.actualizarEstado(pagada.cita().id(), EstadoCita.ATENDIDA);
        citas.registrarPago(pagada.cita().id(), MetodoPago.EFECTIVO,
                Instant.parse("2026-11-18T16:00:00Z"));

        List<CitaDetalle> pendientes = citas.listarPendientesDePago();

        assertEquals(1, pendientes.size());
        assertEquals(aDeber.cita().id(), pendientes.get(0).cita().id());
        assertFalse(pendientes.get(0).cita().id().equals(pendiente.cita().id()));
    }

    @Test
    void debeActualizarEstadoSoloDesdePendiente() throws SQLException {
        CitaDetalle guardada = agendar(LocalDateTime.of(2026, 11, 18, 10, 0), bano);

        assertTrue(citas.actualizarEstado(guardada.cita().id(), EstadoCita.ATENDIDA));
        assertFalse(citas.actualizarEstado(guardada.cita().id(), EstadoCita.CANCELADA));
    }

    @Test
    void debeRegistrarPagoSoloSiEstaAtendidaYADeber() throws SQLException {
        CitaDetalle guardada = agendar(LocalDateTime.of(2026, 11, 18, 10, 0), bano);
        Instant pago = Instant.parse("2026-11-18T16:00:00Z");

        assertFalse(citas.registrarPago(guardada.cita().id(), MetodoPago.EFECTIVO, pago));

        citas.actualizarEstado(guardada.cita().id(), EstadoCita.ATENDIDA);
        assertTrue(citas.registrarPago(guardada.cita().id(), MetodoPago.TRANSFERENCIA, pago));
        assertFalse(citas.registrarPago(guardada.cita().id(), MetodoPago.EFECTIVO, pago));

        CitaDetalle leida = citas.buscarPorId(guardada.cita().id()).orElseThrow();
        assertEquals(MetodoPago.TRANSFERENCIA, leida.cita().metodoPago());
        assertEquals(pago, leida.cita().pagadoEn());
    }

    @Test
    void debeDetectarOtraVisitaNoCanceladaEnElMismoHorario() throws SQLException {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 18, 10, 0);
        CitaDetalle guardada = agendar(fecha, bano);

        assertTrue(citas.existeOtraCitaNoCancelada(mascota.id(), fecha, null));

        citas.actualizarEstado(guardada.cita().id(), EstadoCita.CANCELADA);
        assertFalse(citas.existeOtraCitaNoCancelada(mascota.id(), fecha, null));
    }

    @Test
    void debeHacerRollbackSiFallaUnServicio() throws SQLException {
        Cita cita = Cita.nueva(mascota.id(), LocalDateTime.of(2026, 11, 18, 10, 0), null);
        CitaServicio fantasma = new CitaServicio(null, 9999L, "Fantasma", 100);

        assertThrows(SQLException.class, () -> citas.insertar(cita, List.of(fantasma)));

        try (Connection conexion = database.conexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery("SELECT COUNT(*) FROM citas")) {
            assertTrue(filas.next());
            assertEquals(0, filas.getInt(1), "La visita no debe quedar guardada sin sus servicios");
        }
    }

    @Test
    void debeRechazarInsertarSinServicios() {
        Cita cita = Cita.nueva(mascota.id(), LocalDateTime.of(2026, 11, 18, 10, 0), null);

        assertThrows(IllegalArgumentException.class, () -> citas.insertar(cita, List.of()));
    }

    @Test
    void debeDevolverVacioSiLaVisitaNoExiste() throws SQLException {
        assertTrue(citas.buscarPorId(12345).isEmpty());
    }

    private CitaDetalle agendar(LocalDateTime fecha, Servicio... servicios) throws SQLException {
        List<CitaServicio> congelados = java.util.Arrays.stream(servicios)
                .map(CitaServicio::de)
                .toList();
        return citas.insertar(Cita.nueva(mascota.id(), fecha, null), congelados);
    }
}
