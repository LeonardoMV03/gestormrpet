package net.martinezoft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.ServicioRepository;
import net.martinezoft.domain.Servicio;

class ServicioServiceTest {

    @TempDir
    Path tempDir;

    private ServicioService servicio;

    @BeforeEach
    void prepararServicio() {
        Database database = new Database(tempDir.resolve("servicios-servicio.db"));
        new DatabaseInitializer(database).migrar();
        servicio = new ServicioService(new ServicioRepository(database));
    }

    @Test
    void debeCrearYListarServicios() throws SQLException {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));

        assertTrue(creado.id() != null);
        assertEquals(List.of(creado), servicio.listar());
    }

    @Test
    void debeRechazarServicioNuloAlCrear() {
        assertThrows(NullPointerException.class, () -> servicio.crear(null));
    }

    @Test
    void debeRechazarNombreDuplicadoAlCrear() throws SQLException {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(Servicio.nuevo("Baño", null, 20000, 60)));
        assertEquals(ServicioService.MENSAJE_NOMBRE_DUPLICADO, error.getMessage());
    }

    @Test
    void debeRechazarNombreDuplicadoSinDistinguirMayusculas() throws SQLException {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(Servicio.nuevo("baño", null, 20000, 60)));
    }

    @Test
    void debeRechazarNombreDeUnServicioDesactivado() throws SQLException {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        servicio.desactivar(creado.id());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(Servicio.nuevo("Baño", null, 20000, 60)));
    }

    @Test
    void debePermitirConservarElPropioNombreAlActualizar() throws SQLException {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        Servicio editado = new Servicio(creado.id(), "Baño", "Con secado", 20000, 60, true);

        Servicio actualizado = servicio.actualizar(editado);

        assertEquals(editado, actualizado);
        assertEquals(20000, servicio.buscarPorId(creado.id()).orElseThrow().precioCentavos());
    }

    @Test
    void debeRechazarActualizarConElNombreDeOtroServicio() throws SQLException {
        servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        Servicio corte = servicio.crear(Servicio.nuevo("Corte", null, 20000, 60));

        Servicio editado = new Servicio(corte.id(), "Baño", null, 20000, 60, true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(editado));
        assertEquals(ServicioService.MENSAJE_NOMBRE_DUPLICADO, error.getMessage());
    }

    @Test
    void debeRechazarActualizarUnServicioInexistente() {
        Servicio inexistente = new Servicio(999L, "Baño", null, 15050, 45, true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(inexistente));
        assertEquals("El servicio no existe o está desactivado", error.getMessage());
    }

    @Test
    void debeRechazarActualizarUnServicioSinId() {
        Servicio sinId = Servicio.nuevo("Baño", null, 15050, 45);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(sinId));
        assertEquals("No se puede actualizar un servicio sin id", error.getMessage());
    }

    @Test
    void debeDesactivarUnServicio() throws SQLException {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));

        servicio.desactivar(creado.id());

        assertTrue(servicio.listar().isEmpty());
        assertFalse(servicio.buscarPorId(creado.id()).orElseThrow().activo());
    }

    @Test
    void debeRechazarDesactivarDosVecesElMismoServicio() throws SQLException {
        Servicio creado = servicio.crear(Servicio.nuevo("Baño", null, 15050, 45));
        servicio.desactivar(creado.id());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.desactivar(creado.id()));
        assertEquals("El servicio no existe o ya está desactivado", error.getMessage());
    }

    @Test
    void debeRechazarDesactivarUnServicioInexistente() {
        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(999));
    }
}
