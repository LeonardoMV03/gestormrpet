package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import net.martinezoft.domain.Mascota;
import net.martinezoft.domain.Servicio;
import net.martinezoft.service.CitaService;

class AgendaViewModelTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);

    @TempDir
    Path tempDir;

    private CitaService citas;
    private RelativeClock reloj;
    private AgendaViewModel vm;
    private Mascota mascota;
    private Servicio bano;
    private Servicio corte;

    @BeforeEach
    void prepararViewModel() throws SQLException {
        Database database = new Database(tempDir.resolve("agenda-vm.db"));
        new DatabaseInitializer(database).migrar();
        ClienteRepository clientes = new ClienteRepository(database);
        MascotaRepository mascotas = new MascotaRepository(database);
        ServicioRepository servicios = new ServicioRepository(database);
        reloj = new RelativeClock(Instant.parse("2026-11-18T12:00:00Z"));
        citas = new CitaService(new CitaRepository(database), mascotas, clientes, servicios, reloj);

        Cliente cliente = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null));
        mascota = mascotas.insertar(Mascota.nueva(cliente.id(), "Luna", null, null, null, null, null));
        bano = servicios.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        corte = servicios.insertar(Servicio.nuevo("Corte", null, 20000, 60));
        vm = new AgendaViewModel(citas, reloj);
    }

    @Test
    void debeCargarMascotasYServicios() {
        vm.cargar();

        assertEquals(1, vm.mascotas().size());
        assertEquals(2, vm.servicios().size());
        assertEquals(HOY, vm.fechaCitaProperty().get());
        assertTrue(vm.citasHoy().isEmpty());
        assertTrue(vm.citasProximas().isEmpty());
    }

    @Test
    void debeCalcularElTotalDeLosServiciosSeleccionados() {
        vm.cargar();

        vm.serviciosSeleccionados().setAll(bano, corte);

        assertEquals(35050, vm.totalSeleccionadoProperty().get());
        assertFalse(vm.puedeAgendarProperty().get());
    }

    @Test
    void debeAgendarUnaVisitaYLimpiarElFormulario() {
        vm.cargar();
        prepararFormulario(bano);

        assertTrue(vm.puedeAgendarProperty().get());
        vm.agendar();

        assertEquals(AgendaViewModel.MENSAJE_AGENDADA, vm.mensajeProperty().get());
        assertEquals(1, vm.citasHoy().size());
        assertNotNull(vm.seleccionadaProperty().get());
        assertTrue(vm.serviciosSeleccionados().isEmpty());
        assertEquals("", vm.horaTextoProperty().get());
        assertEquals("", vm.notasProperty().get());
        assertEquals(0, vm.totalSeleccionadoProperty().get());
    }

    @Test
    void debeMostrarErrorConHoraInvalida() {
        vm.cargar();
        prepararFormulario(bano);
        vm.horaTextoProperty().set("25:99");

        assertFalse(vm.puedeAgendarProperty().get());
        vm.agendar();

        assertFalse(vm.errorFechaHoraProperty().get().isBlank());
        assertTrue(vm.citasHoy().isEmpty());
    }

    @Test
    void debeMostrarErrorSinMascota() {
        vm.cargar();
        vm.fechaCitaProperty().set(HOY);
        vm.horaTextoProperty().set("10:00");
        vm.serviciosSeleccionados().setAll(bano);

        assertFalse(vm.puedeAgendarProperty().get(), "Sin mascota no se puede agendar");
        vm.agendar();

        assertEquals(CitaService.MENSAJE_MASCOTA_INVALIDA, vm.errorMascotaProperty().get());
        assertTrue(vm.citasHoy().isEmpty());
    }

    @Test
    void debeMostrarErrorSiLaMascotaYaTieneVisitaEnEseHorario() {
        vm.cargar();
        prepararFormulario(bano);
        vm.agendar();

        prepararFormulario(corte);
        vm.agendar();

        assertEquals(CitaService.MENSAJE_HORARIO_OCUPADO, vm.errorFechaHoraProperty().get());
        assertEquals(1, vm.citasHoy().size());
    }

    @Test
    void debeAtenderYCobrarLaVisitaSeleccionada() {
        vm.cargar();
        prepararFormulario(bano);
        vm.agendar();
        vm.seleccionar(vm.citasHoy().get(0));

        vm.atenderSeleccionada();

        assertEquals(AgendaViewModel.MENSAJE_ATENDIDA, vm.mensajeProperty().get());
        assertTrue(vm.seleccionadaProperty().get().cita().estaAtendida());
        assertTrue(vm.puedeCobrarProperty().get());

        vm.registrarPagoSeleccionado();

        assertEquals(AgendaViewModel.MENSAJE_COBRADA, vm.mensajeProperty().get());
        assertTrue(vm.seleccionadaProperty().get().cita().estaPagada());
        assertFalse(vm.puedeCobrarProperty().get());
    }

    @Test
    void debeRechazarCobrarUnaVisitaPendiente() {
        vm.cargar();
        prepararFormulario(bano);
        vm.agendar();
        vm.seleccionar(vm.citasHoy().get(0));

        vm.registrarPagoSeleccionado();

        assertFalse(vm.errorGeneralProperty().get().isBlank());
        assertTrue(vm.citasHoy().get(0).cita().estaPendiente());
    }

    @Test
    void debeCancelarLaVisitaSeleccionada() {
        vm.cargar();
        prepararFormulario(bano);
        vm.agendar();
        vm.seleccionar(vm.citasHoy().get(0));

        vm.cancelarSeleccionada();

        assertEquals(AgendaViewModel.MENSAJE_CANCELADA, vm.mensajeProperty().get());
        assertTrue(vm.seleccionadaProperty().get().cita().estado() == net.martinezoft.domain.EstadoCita.CANCELADA);
    }

    @Test
    void debeCargarLasVisitasProximas() throws SQLException {
        CitaDetalle manana = citas.agendar(mascota.id(), List.of(corte.id()),
                HOY.plusDays(1).atTime(9, 0), null);

        vm.cargar();

        assertTrue(vm.citasHoy().isEmpty());
        assertEquals(1, vm.citasProximas().size());
        assertEquals(manana.cita().id(), vm.citasProximas().get(0).cita().id());
    }

    @Test
    void debeLimpiarElFormularioConNuevo() {
        vm.cargar();
        prepararFormulario(bano);

        vm.nuevo();

        assertNull(vm.seleccionadaProperty().get());
        assertNull(vm.mascotaSeleccionadaProperty().get());
        assertTrue(vm.serviciosSeleccionados().isEmpty());
        assertEquals("", vm.horaTextoProperty().get());
        assertEquals("", vm.notasProperty().get());
        assertEquals(HOY, vm.fechaCitaProperty().get());
    }

    private void prepararFormulario(Servicio... servicios) {
        vm.mascotaSeleccionadaProperty().set(vm.mascotas().get(0));
        vm.fechaCitaProperty().set(HOY);
        vm.horaTextoProperty().set("10:00");
        vm.notasProperty().set("Visita de prueba");
        vm.serviciosSeleccionados().setAll(servicios);
    }

    private static final class RelativeClock extends java.time.Clock {

        private final Instant instante;

        RelativeClock(Instant instante) {
            this.instante = instante;
        }

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
