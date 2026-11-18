package net.martinezoft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.data.CitaRepository;
import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.MascotaRepository;
import net.martinezoft.data.ServicioRepository;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Deudor;
import net.martinezoft.domain.EstadoCita;
import net.martinezoft.domain.Mascota;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.domain.Servicio;

class CitaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);

    @TempDir
    Path tempDir;

    private ClienteRepository clientes;
    private MascotaRepository mascotas;
    private ServicioRepository servicios;
    private CitaService citas;
    private Cliente cliente;
    private Mascota mascota;
    private Servicio bano;
    private Servicio corte;

    @BeforeEach
    void prepararServicio() throws SQLException {
        Database database = new Database(tempDir.resolve("citas-servicio.db"));
        new DatabaseInitializer(database).migrar();
        clientes = new ClienteRepository(database);
        mascotas = new MascotaRepository(database);
        servicios = new ServicioRepository(database);
        citas = new CitaService(new CitaRepository(database), mascotas, clientes, servicios,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        cliente = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null));
        mascota = mascotas.insertar(Mascota.nueva(cliente.id(), "Luna", null, null, null, null, null));
        bano = servicios.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        corte = servicios.insertar(Servicio.nuevo("Corte", null, 20000, 60));
    }

    @Test
    void debeAgendarYCongelarLosPreciosDelCatalogo() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id(), corte.id()),
                HOY.atTime(10, 0), "Primera visita");

        assertNotNull(creada.cita().id());
        assertEquals(35050, creada.totalCentavos());
        assertEquals(2, creada.servicios().size());
        assertEquals(EstadoCita.PENDIENTE, creada.cita().estado());

        servicios.actualizar(new Servicio(bano.id(), "Baño", null, 90000, 45, true));

        CitaDetalle leida = citas.buscar(creada.cita().id());
        assertEquals(15050, leida.servicios().stream()
                .filter(servicio -> servicio.servicioId().equals(bano.id()))
                .findFirst().orElseThrow().precioCentavos());
    }

    @Test
    void debeRechazarVisitaSinServicios() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(mascota.id(), List.of(), HOY.atTime(10, 0), null));

        assertEquals(CitaService.MENSAJE_SIN_SERVICIOS, error.getMessage());
    }

    @Test
    void debeRechazarServicioDuplicado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(mascota.id(), List.of(bano.id(), bano.id()),
                        HOY.atTime(10, 0), null));

        assertEquals(CitaService.MENSAJE_SERVICIO_DUPLICADO, error.getMessage());
    }

    @Test
    void debeRechazarServicioInactivo() throws SQLException {
        servicios.desactivar(bano.id());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null));

        assertEquals(CitaService.MENSAJE_SERVICIO_INVALIDO, error.getMessage());
    }

    @Test
    void debeRechazarMascotaDesactivadaOInexistente() throws SQLException {
        mascotas.desactivar(mascota.id());

        IllegalArgumentException desactivada = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null));
        IllegalArgumentException inexistente = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(999L, List.of(bano.id()), HOY.atTime(10, 0), null));

        assertEquals(CitaService.MENSAJE_MASCOTA_INVALIDA, desactivada.getMessage());
        assertEquals(CitaService.MENSAJE_MASCOTA_INVALIDA, inexistente.getMessage());
    }

    @Test
    void debeRechazarOtraVisitaDeLaMismaMascotaEnElMismoHorario() throws SQLException {
        LocalDateTime fecha = HOY.atTime(10, 0);
        citas.agendar(mascota.id(), List.of(bano.id()), fecha, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citas.agendar(mascota.id(), List.of(corte.id()), fecha, null));

        assertEquals(CitaService.MENSAJE_HORARIO_OCUPADO, error.getMessage());
    }

    @Test
    void debePermitirElHorarioSiLaVisitaAnteriorEstaCancelada() throws SQLException {
        LocalDateTime fecha = HOY.atTime(10, 0);
        CitaDetalle primera = citas.agendar(mascota.id(), List.of(bano.id()), fecha, null);
        citas.cancelar(primera.cita().id());

        CitaDetalle segunda = citas.agendar(mascota.id(), List.of(corte.id()), fecha, null);

        assertNotNull(segunda.cita().id());
        assertEquals(EstadoCita.PENDIENTE, segunda.cita().estado());
    }

    @Test
    void debeAtenderYRegistrarElPago() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);

        CitaDetalle atendida = citas.atender(creada.cita().id());
        CitaDetalle pagada = citas.registrarPago(creada.cita().id(), MetodoPago.EFECTIVO);

        assertEquals(EstadoCita.ATENDIDA, atendida.cita().estado());
        assertTrue(pagada.cita().estaPagada());
        assertEquals(MetodoPago.EFECTIVO, pagada.cita().metodoPago());
        assertEquals(AHORA, pagada.cita().pagadoEn());
    }

    @Test
    void debeRechazarPagoSiLaVisitaNoEstaAtendida() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);

        assertThrows(IllegalArgumentException.class,
                () -> citas.registrarPago(creada.cita().id(), MetodoPago.EFECTIVO));
    }

    @Test
    void debeRechazarPagarDosVecesLaMismaVisita() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);
        citas.atender(creada.cita().id());
        citas.registrarPago(creada.cita().id(), MetodoPago.EFECTIVO);

        assertThrows(IllegalArgumentException.class,
                () -> citas.registrarPago(creada.cita().id(), MetodoPago.TRANSFERENCIA));
    }

    @Test
    void debeRechazarCancelarUnaVisitaAtendida() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);
        citas.atender(creada.cita().id());

        assertThrows(IllegalArgumentException.class, () -> citas.cancelar(creada.cita().id()));
    }

    @Test
    void debeListarHoyYProximas() throws SQLException {
        CitaDetalle deHoy = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);
        CitaDetalle deManana = citas.agendar(mascota.id(), List.of(corte.id()),
                HOY.plusDays(1).atTime(9, 0), null);

        List<CitaDetalle> hoy = citas.listarHoy(HOY);
        List<CitaDetalle> proximas = citas.listarProximas(HOY);

        assertEquals(List.of(deHoy.cita().id()), hoy.stream().map(cita -> cita.cita().id()).toList());
        assertEquals(List.of(deManana.cita().id()),
                proximas.stream().map(cita -> cita.cita().id()).toList());
    }

    @Test
    void debeAgruparLosDeudoresPorClienteYSumarSusTotales() throws SQLException {
        Cliente bruno = clientes.insertar(Cliente.nuevo("Bruno", null, null, null, null));
        Mascota michi = mascotas.insertar(Mascota.nueva(bruno.id(), "Michi", null, null, null, null, null));

        CitaDetalle anaUno = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(9, 0), null);
        CitaDetalle anaDos = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);
        CitaDetalle brunoUno = citas.agendar(michi.id(), List.of(corte.id()), HOY.atTime(11, 0), null);
        for (CitaDetalle cita : List.of(anaUno, anaDos, brunoUno)) {
            citas.atender(cita.cita().id());
        }

        List<Deudor> deudores = citas.listarDeudores();

        assertEquals(2, deudores.size());
        assertEquals("Ana", deudores.get(0).clienteNombre());
        assertEquals(2, deudores.get(0).visitas());
        assertEquals(30100, deudores.get(0).totalCentavos());
        assertEquals("Bruno", deudores.get(1).clienteNombre());
        assertEquals(20000, deudores.get(1).totalCentavos());
    }

    @Test
    void debeQuitarAlClienteDeDeudoresCuandoPaga() throws SQLException {
        CitaDetalle creada = citas.agendar(mascota.id(), List.of(bano.id()), HOY.atTime(10, 0), null);
        citas.atender(creada.cita().id());

        citas.registrarPago(creada.cita().id(), MetodoPago.TRANSFERENCIA);

        assertTrue(citas.listarDeudores().isEmpty());
        assertTrue(citas.listarCobrosPendientes().isEmpty());
    }

    @Test
    void debeListarMascotasYServiciosDisponibles() throws SQLException {
        assertEquals(1, citas.listarMascotasDisponibles().size());
        assertEquals("Luna · Ana", citas.listarMascotasDisponibles().get(0).etiqueta());
        assertEquals(2, citas.listarServiciosDisponibles().size());

        servicios.desactivar(bano.id());

        assertEquals(1, citas.listarServiciosDisponibles().size());
        assertFalse(citas.listarServiciosDisponibles().isEmpty());
    }
}
