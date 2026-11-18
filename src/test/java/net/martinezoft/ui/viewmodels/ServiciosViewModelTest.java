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

import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.ServicioRepository;
import net.martinezoft.domain.Servicio;
import net.martinezoft.service.ServicioService;

class ServiciosViewModelTest {

    @TempDir
    Path tempDir;

    private ServicioService servicio;
    private ServiciosViewModel vm;

    @BeforeEach
    void prepararViewModel() {
        Database database = new Database(tempDir.resolve("servicios-vm.db"));
        new DatabaseInitializer(database).migrar();
        servicio = new ServicioService(new ServicioRepository(database));
        vm = new ServiciosViewModel(servicio);
    }

    @Test
    void debeCargarServiciosActivosAlIniciar() throws Exception {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        servicio.crear(Servicio.nuevo("Corte", null, 20000, null));

        vm.cargar();

        assertEquals(2, vm.servicios().size());
        assertEquals("Baño", vm.servicios().get(0).nombre());
        assertEquals("Corte", vm.servicios().get(1).nombre());
    }

    @Test
    void debeCalcularPuedeGuardarSegunLosCampos() {
        assertFalse(vm.puedeGuardarProperty().get(), "Sin campos no debe permitir guardar");

        vm.nombreProperty().set("Baño");
        assertFalse(vm.puedeGuardarProperty().get(), "Falta el precio");

        vm.precioTextoProperty().set("150,50");
        assertTrue(vm.puedeGuardarProperty().get());

        vm.duracionTextoProperty().set("45");
        assertTrue(vm.puedeGuardarProperty().get());

        vm.duracionTextoProperty().set("x");
        assertFalse(vm.puedeGuardarProperty().get(), "Una duración no numérica no es válida");

        vm.duracionTextoProperty().set("");
        vm.precioTextoProperty().set("-1");
        assertFalse(vm.puedeGuardarProperty().get(), "Un precio negativo no es válido");
    }

    @Test
    void debeGuardarServicioNuevoEnCentavos() throws Exception {
        vm.nombreProperty().set("Baño");
        vm.descripcionProperty().set("Baño completo");
        vm.precioTextoProperty().set("150,50");
        vm.duracionTextoProperty().set("45");

        vm.guardar();

        assertEquals(ServiciosViewModel.MENSAJE_GUARDADO, vm.mensajeProperty().get());
        assertEquals(1, vm.servicios().size());
        Servicio guardado = vm.seleccionadoProperty().get();
        assertNotNull(guardado);
        assertNotNull(guardado.id());
        assertEquals(15050, guardado.precioCentavos());
        assertEquals(45, guardado.duracionMin().intValue());
        assertEquals(15050, servicio.listar().get(0).precioCentavos());
        assertTrue(vm.editandoProperty().get());
    }

    @Test
    void debeVolcarElServicioSeleccionadoEnElFormulario() throws Exception {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", "Baño completo", 15050, 45));
        vm.cargar();

        vm.seleccionar(vm.servicios().get(0));

        assertEquals("Baño", vm.nombreProperty().get());
        assertEquals("Baño completo", vm.descripcionProperty().get());
        assertEquals("150,50", vm.precioTextoProperty().get());
        assertEquals("45", vm.duracionTextoProperty().get());
        assertEquals(creado.id(), vm.seleccionadoProperty().get().id());
        assertTrue(vm.editandoProperty().get());
    }

    @Test
    void debeVaciarLaDuracionCuandoElServicioNoLaTiene() throws Exception {
        servicio.crear(Servicio.nuevo("Corte", null, 20000, null));
        vm.cargar();

        vm.seleccionar(vm.servicios().get(0));

        assertEquals("", vm.duracionTextoProperty().get());
        assertEquals("", vm.descripcionProperty().get());
    }

    @Test
    void debeActualizarElServicioSeleccionado() throws Exception {
        servicio.crear(Servicio.nuevo("Baño", "Baño completo", 15050, 45));
        vm.cargar();
        vm.seleccionar(vm.servicios().get(0));

        vm.precioTextoProperty().set("160");
        vm.duracionTextoProperty().set("");
        vm.guardar();

        assertEquals(1, vm.servicios().size());
        Servicio actualizado = servicio.listar().get(0);
        assertEquals(16000, actualizado.precioCentavos());
        assertNull(actualizado.duracionMin());
        assertEquals(ServiciosViewModel.MENSAJE_GUARDADO, vm.mensajeProperty().get());
    }

    @Test
    void debeMostrarErrorDePrecioInvalidoAlGuardar() {
        vm.nombreProperty().set("Baño");
        vm.precioTextoProperty().set("abc");

        assertFalse(vm.puedeGuardarProperty().get());
        vm.guardar();

        assertFalse(vm.errorPrecioProperty().get().isBlank());
        assertEquals("", vm.errorGeneralProperty().get());
        assertTrue(vm.servicios().isEmpty());
    }

    @Test
    void debeMostrarErrorDeDuracionInvalidaAlGuardar() {
        vm.nombreProperty().set("Baño");
        vm.precioTextoProperty().set("150");
        vm.duracionTextoProperty().set("x");

        assertFalse(vm.puedeGuardarProperty().get());
        vm.guardar();

        assertFalse(vm.errorDuracionProperty().get().isBlank());
        assertTrue(vm.servicios().isEmpty());
    }

    @Test
    void debeMapearNombreDuplicadoAErrorNombre() throws Exception {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        vm.cargar();

        vm.nombreProperty().set("Baño");
        vm.precioTextoProperty().set("200");
        vm.guardar();

        assertEquals(ServicioService.MENSAJE_NOMBRE_DUPLICADO, vm.errorNombreProperty().get());
        assertEquals(1, vm.servicios().size());
    }

    @Test
    void debeDesactivarServicioYLimpiarElFormulario() throws Exception {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        vm.cargar();
        vm.seleccionar(vm.servicios().get(0));

        vm.desactivarSeleccionado();

        assertTrue(vm.servicios().isEmpty());
        assertNull(vm.seleccionadoProperty().get());
        assertEquals("", vm.nombreProperty().get());
        assertEquals("", vm.precioTextoProperty().get());
        assertFalse(vm.editandoProperty().get());
        assertEquals(ServiciosViewModel.MENSAJE_DESACTIVADO, vm.mensajeProperty().get());
        assertFalse(servicio.buscarPorId(creado.id()).orElseThrow().activo());
    }

    @Test
    void debeConservarLaSeleccionAlRecargar() throws Exception {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        servicio.crear(Servicio.nuevo("Corte", null, 20000, null));
        vm.cargar();
        vm.seleccionar(vm.servicios().get(1));

        vm.cargar();

        assertNotNull(vm.seleccionadoProperty().get());
        assertEquals("Corte", vm.seleccionadoProperty().get().nombre());
    }

    @Test
    void debeLimpiarElFormularioConNuevo() throws Exception {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        vm.cargar();
        vm.seleccionar(vm.servicios().get(0));

        vm.nuevo();

        assertNull(vm.seleccionadoProperty().get());
        assertEquals("", vm.nombreProperty().get());
        assertEquals("", vm.precioTextoProperty().get());
        assertEquals("", vm.duracionTextoProperty().get());
        assertFalse(vm.editandoProperty().get());
    }

    @Test
    void debeMostrarErrorTecnicoSiFallaLaBaseDeDatos() {
        Path inexistente = tempDir.resolve("no-existe").resolve("bd.db");
        ServicioService servicioRoto = new ServicioService(
                new ServicioRepository(Database.deUrl("jdbc:sqlite:" + inexistente)));
        ServiciosViewModel conFallo = new ServiciosViewModel(servicioRoto);

        conFallo.cargar();
        assertEquals(ServiciosViewModel.MENSAJE_ERROR_CARGA, conFallo.errorGeneralProperty().get());

        conFallo.nombreProperty().set("Baño");
        conFallo.precioTextoProperty().set("150");
        conFallo.guardar();

        assertEquals(ServiciosViewModel.MENSAJE_ERROR_TECNICO, conFallo.errorGeneralProperty().get());
    }
}
