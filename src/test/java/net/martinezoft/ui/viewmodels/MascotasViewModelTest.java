package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.MascotaRepository;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Mascota;
import net.martinezoft.service.MascotaService;

class MascotasViewModelTest {

    @TempDir
    Path tempDir;

    private ClienteRepository clientes;
    private MascotaService servicio;
    private MascotasViewModel vm;
    private Long clienteId;

    @BeforeEach
    void prepararViewModel() throws Exception {
        Database database = new Database(tempDir.resolve("mascotas-vm.db"));
        new DatabaseInitializer(database).migrar();
        clientes = new ClienteRepository(database);
        servicio = new MascotaService(new MascotaRepository(database), clientes);
        vm = new MascotasViewModel(servicio);
        clienteId = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null)).id();
    }

    @Test
    void debeCargarLasMascotasDelCliente() throws Exception {
        servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        servicio.crear(Mascota.nueva(clienteId, "Luna", null, null, null, null, null));

        vm.cargarPara(clienteId);

        assertEquals(2, vm.mascotas().size());
        assertTrue(vm.tieneCliente());
    }

    @Test
    void debeVaciarLaListaAlNoTenerCliente() {
        vm.cargarPara(null);

        assertTrue(vm.mascotas().isEmpty());
        assertFalse(vm.tieneCliente());
        assertFalse(vm.puedeGuardarProperty().get());
    }

    @Test
    void debeCalcularPuedeGuardarConClienteYNombre() {
        vm.cargarPara(clienteId);
        assertFalse(vm.puedeGuardarProperty().get());

        vm.nombreProperty().set("Firulais");

        assertTrue(vm.puedeGuardarProperty().get());
    }

    @Test
    void debeGuardarMascotaNuevaEnElClienteActual() throws Exception {
        vm.cargarPara(clienteId);
        vm.nombreProperty().set("Firulais");
        vm.especieProperty().set("gato");

        vm.guardar();

        assertEquals(1, vm.mascotas().size());
        assertEquals("gato", vm.mascotas().get(0).especie());
        assertEquals(MascotasViewModel.MENSAJE_GUARDADO, vm.mensajeProperty().get());
    }

    @Test
    void debeRechazarGuardarSinCliente() {
        vm.guardar();

        assertEquals(MascotasViewModel.MENSAJE_SIN_CLIENTE, vm.errorGeneralProperty().get());
    }

    @Test
    void debeVolcarLaMascotaSeleccionadaEnElFormulario() throws Exception {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", "perro",
                "Beagle", "macho", "2020-05-10", null));
        vm.cargarPara(clienteId);

        vm.seleccionar(creada);

        assertEquals("Firulais", vm.nombreProperty().get());
        assertEquals("perro", vm.especieProperty().get());
        assertEquals("Beagle", vm.razaProperty().get());
        assertEquals("macho", vm.sexoProperty().get());
        assertEquals("2020-05-10", vm.fechaNacimientoProperty().get());
        assertTrue(vm.editandoProperty().get());
    }

    @Test
    void debeActualizarLaMascotaSeleccionada() throws Exception {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        vm.cargarPara(clienteId);
        vm.seleccionar(creada);

        vm.nombreProperty().set("Firulais II");
        vm.guardar();

        assertEquals("Firulais II", servicio.buscarPorId(creada.id()).orElseThrow().nombre());
    }

    @Test
    void debeMostrarErrorDeFechaInvalida() throws Exception {
        vm.cargarPara(clienteId);
        vm.nombreProperty().set("Firulais");
        vm.fechaNacimientoProperty().set("ayer");

        vm.guardar();

        assertFalse(vm.errorFechaProperty().get().isBlank());
        assertTrue(vm.mascotas().isEmpty());
    }

    @Test
    void debeDesactivarLaMascotaSeleccionada() throws Exception {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        vm.cargarPara(clienteId);
        vm.seleccionar(creada);

        vm.desactivarSeleccionada();

        assertTrue(vm.mascotas().isEmpty());
        assertEquals(MascotasViewModel.MENSAJE_DESACTIVADA, vm.mensajeProperty().get());
        assertNull(vm.seleccionadaProperty().get());
    }

    @Test
    void debeRechazarDesactivarSinSeleccion() {
        vm.cargarPara(clienteId);

        vm.desactivarSeleccionada();

        assertEquals(MascotasViewModel.MENSAJE_SIN_SELECCION, vm.errorGeneralProperty().get());
    }

    @Test
    void debeLimpiarElFormularioAlCambiarDeCliente() throws Exception {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        vm.cargarPara(clienteId);
        vm.seleccionar(creada);
        Long otroCliente = clientes.insertar(Cliente.nuevo("Bruno", null, null, null, null)).id();

        vm.cargarPara(otroCliente);

        assertEquals("", vm.nombreProperty().get());
        assertNull(vm.seleccionadaProperty().get());
        assertTrue(vm.mascotas().isEmpty());
    }
}
