package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.domain.Cliente;
import net.martinezoft.service.ClienteService;

class ClientesViewModelTest {

    @TempDir
    Path tempDir;

    private ClienteService servicio;
    private ClientesViewModel vm;

    @BeforeEach
    void prepararViewModel() {
        Database database = new Database(tempDir.resolve("clientes-vm.db"));
        new DatabaseInitializer(database).migrar();
        servicio = new ClienteService(new ClienteRepository(database));
        vm = new ClientesViewModel(servicio);
    }

    @Test
    void debeCargarClientesAlIniciar() throws Exception {
        servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        servicio.crear(Cliente.nuevo("Bruno", null, null, null, null));

        vm.cargar();

        assertEquals(2, vm.clientes().size());
        assertEquals("Ana", vm.clientes().get(0).nombre());
    }

    @Test
    void debeFiltrarAlCambiarLaBusqueda() throws Exception {
        servicio.crear(Cliente.nuevo("Ana Pérez", "555-1234", null, null, null));
        servicio.crear(Cliente.nuevo("Bruno", "555-9999", null, null, null));
        vm.cargar();

        vm.busquedaProperty().set("1234");

        assertEquals(1, vm.clientes().size());
        assertEquals("Ana Pérez", vm.clientes().get(0).nombre());
    }

    @Test
    void debeCalcularPuedeGuardarSoloConNombre() {
        assertFalse(vm.puedeGuardarProperty().get());

        vm.nombreProperty().set("Ana");

        assertTrue(vm.puedeGuardarProperty().get());
    }

    @Test
    void debeGuardarClienteNuevo() throws Exception {
        vm.nombreProperty().set("  Ana  ");
        vm.telefonoProperty().set("555");

        vm.guardar();

        assertEquals(1, vm.clientes().size());
        assertEquals("Ana", vm.clientes().get(0).nombre());
        assertEquals(ClientesViewModel.MENSAJE_GUARDADO, vm.mensajeProperty().get());
        assertTrue(vm.editandoProperty().get());
    }

    @Test
    void debeVolcarElClienteSeleccionadoEnElFormulario() throws Exception {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", "555", "ana@x.com", null, null));
        vm.cargar();

        vm.seleccionar(creado);

        assertEquals("Ana", vm.nombreProperty().get());
        assertEquals("555", vm.telefonoProperty().get());
        assertEquals("ana@x.com", vm.emailProperty().get());
        assertTrue(vm.editandoProperty().get());
    }

    @Test
    void debeActualizarElClienteSeleccionado() throws Exception {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        vm.cargar();
        vm.seleccionar(creado);

        vm.nombreProperty().set("Ana Pérez");
        vm.guardar();

        assertEquals("Ana Pérez", servicio.buscarPorId(creado.id()).orElseThrow().nombre());
        assertNotNull(vm.seleccionadoProperty().get());
    }

    @Test
    void debeMostrarErrorDeNombreVacioAlGuardar() {
        vm.guardar();

        assertFalse(vm.errorNombreProperty().get().isBlank());
        assertTrue(vm.clientes().isEmpty());
    }

    @Test
    void debeLimpiarElFormularioConNuevo() throws Exception {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        vm.cargar();
        vm.seleccionar(creado);

        vm.nuevo();

        assertEquals("", vm.nombreProperty().get());
        assertFalse(vm.editandoProperty().get());
        assertNull(vm.seleccionadoProperty().get());
    }

    @Test
    void debeDesactivarElClienteSeleccionado() throws Exception {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        vm.cargar();
        vm.seleccionar(creado);

        vm.desactivarSeleccionado();

        assertTrue(vm.clientes().isEmpty());
        assertEquals(ClientesViewModel.MENSAJE_DESACTIVADO, vm.mensajeProperty().get());
        assertNull(vm.seleccionadoProperty().get());
    }

    @Test
    void debeRechazarDesactivarSinSeleccion() {
        vm.desactivarSeleccionado();

        assertEquals(ClientesViewModel.MENSAJE_SIN_SELECCION, vm.errorGeneralProperty().get());
    }

    @Test
    void debeLimpiarLaSeleccionSiElClienteDesapareceDelFiltro() throws Exception {
        servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        vm.cargar();
        vm.seleccionar(vm.clientes().get(0));

        vm.busquedaProperty().set("Bruno");

        assertTrue(vm.clientes().isEmpty());
        assertNull(vm.seleccionadoProperty().get());
        assertEquals("", vm.nombreProperty().get());
    }
}
