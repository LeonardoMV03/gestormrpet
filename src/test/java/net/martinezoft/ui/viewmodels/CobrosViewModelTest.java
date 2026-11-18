package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import net.martinezoft.domain.Mascota;
import net.martinezoft.domain.Servicio;
import net.martinezoft.service.CitaService;

class CobrosViewModelTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);

    @TempDir
    Path tempDir;

    private CitaService citas;
    private CobrosViewModel vm;
    private Cliente ana;
    private Cliente bruno;
    private Mascota luna;
    private Mascota michi;
    private Servicio bano;
    private Servicio corte;

    @BeforeEach
    void prepararViewModel() throws SQLException {
        Database database = new Database(tempDir.resolve("cobros-vm.db"));
        new DatabaseInitializer(database).migrar();
        ClienteRepository clientes = new ClienteRepository(database);
        MascotaRepository mascotas = new MascotaRepository(database);
        ServicioRepository servicios = new ServicioRepository(database);
        citas = new CitaService(new CitaRepository(database), mascotas, clientes, servicios,
                Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC));

        ana = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null));
        bruno = clientes.insertar(Cliente.nuevo("Bruno", null, null, null, null));
        luna = mascotas.insertar(Mascota.nueva(ana.id(), "Luna", null, null, null, null, null));
        michi = mascotas.insertar(Mascota.nueva(bruno.id(), "Michi", null, null, null, null, null));
        bano = servicios.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        corte = servicios.insertar(Servicio.nuevo("Corte", null, 20000, 60));
        vm = new CobrosViewModel(citas);
    }

    @Test
    void debeCargarDeudoresYTotalGeneral() throws SQLException {
        crearDeuda(luna, bano);

        vm.cargar();

        assertEquals(1, vm.deudores().size());
        assertEquals("Ana", vm.deudores().get(0).clienteNombre());
        assertEquals(15050, vm.totalGeneralProperty().get());
        assertEquals(1, vm.visitas().size());
    }

    @Test
    void debeFiltrarLasVisitasDelDeudorSeleccionado() throws SQLException {
        crearDeuda(luna, bano);
        crearDeuda(michi, corte);
        vm.cargar();

        assertEquals(2, vm.visitas().size());

        vm.seleccionarDeudor(vm.deudores().get(0));

        assertEquals(1, vm.visitas().size());
        assertEquals("Ana", vm.visitas().get(0).clienteNombre());

        vm.seleccionarDeudor(null);

        assertEquals(2, vm.visitas().size());
    }

    @Test
    void debeRegistrarPagoYQuitarAlDeudor() throws SQLException {
        crearDeuda(luna, bano);
        vm.cargar();
        vm.seleccionarVisita(vm.visitas().get(0));

        vm.registrarPagoSeleccionado();

        assertEquals(CobrosViewModel.MENSAJE_COBRADA, vm.mensajeProperty().get());
        assertTrue(vm.deudores().isEmpty());
        assertTrue(vm.visitas().isEmpty());
        assertEquals(0, vm.totalGeneralProperty().get());
        assertNull(vm.visitaSeleccionadaProperty().get());
    }

    @Test
    void debeMostrarErrorSinVisitaSeleccionada() {
        vm.cargar();

        vm.registrarPagoSeleccionado();

        assertEquals(CobrosViewModel.MENSAJE_SIN_SELECCION, vm.errorGeneralProperty().get());
    }

    @Test
    void debeDeshabilitarElCobroSinSeleccion() throws SQLException {
        crearDeuda(luna, bano);
        vm.cargar();

        assertFalse(vm.puedeCobrarProperty().get());

        vm.seleccionarVisita(vm.visitas().get(0));

        assertTrue(vm.puedeCobrarProperty().get());
    }

    private void crearDeuda(Mascota mascota, Servicio servicio) throws SQLException {
        CitaDetalle cita = citas.agendar(mascota.id(), List.of(servicio.id()), HOY.atTime(10, 0), null);
        citas.atender(cita.cita().id());
    }
}
