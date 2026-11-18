package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Servicio;

class ServicioRepositoryTest {

    @TempDir
    Path tempDir;

    private ServicioRepository repositorio;

    @BeforeEach
    void prepararRepositorio() {
        Database database = new Database(tempDir.resolve("servicios.db"));
        new DatabaseInitializer(database).migrar();
        repositorio = new ServicioRepository(database);
    }

    @Test
    void debeInsertarYBuscarPorId() throws SQLException {
        Servicio guardado = repositorio.insertar(
                Servicio.nuevo("Baño", "Baño completo", 15050, 45));

        assertNotNull(guardado.id(), "El repositorio debe devolver el id generado");
        Servicio encontrado = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertEquals(guardado, encontrado);
        assertTrue(encontrado.activo());
    }

    @Test
    void debeListarSoloActivosOrdenadosPorNombreSinDistinguirMayusculas() throws SQLException {
        Servicio zeta = repositorio.insertar(Servicio.nuevo("zeta", null, 100, null));
        Servicio alfa = repositorio.insertar(Servicio.nuevo("Alfa", null, 100, null));
        Servicio beta = repositorio.insertar(Servicio.nuevo("beta", null, 100, null));

        assertEquals(List.of(alfa, beta, zeta), repositorio.listar());

        repositorio.desactivar(beta.id());

        assertEquals(List.of(alfa, zeta), repositorio.listar());
    }

    @Test
    void debeActualizarUnServicioActivo() throws SQLException {
        Servicio guardado = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        Servicio editado = new Servicio(guardado.id(), "Baño premium", "Con secado",
                20000, 60, true);

        assertTrue(repositorio.actualizar(editado));

        assertEquals(editado, repositorio.buscarPorId(guardado.id()).orElseThrow());
    }

    @Test
    void debeDesactivarConSoftDelete() throws SQLException {
        Servicio guardado = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));

        assertTrue(repositorio.desactivar(guardado.id()));
        assertFalse(repositorio.desactivar(guardado.id()), "Desactivar dos veces no debe afectar filas");

        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void debeBuscarPorIdIncluyendoInactivos() throws SQLException {
        Servicio guardado = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        repositorio.desactivar(guardado.id());

        Servicio inactivo = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertFalse(inactivo.activo());
    }

    @Test
    void noDebeActualizarUnServicioInactivo() throws SQLException {
        Servicio guardado = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));
        repositorio.desactivar(guardado.id());

        Servicio editado = new Servicio(guardado.id(), "Baño premium", null, 20000, 60, true);

        assertFalse(repositorio.actualizar(editado), "Un servicio inactivo no debe actualizarse");
    }

    @Test
    void debePersistirDuracionNulaYDuracionInformada() throws SQLException {
        Servicio sinDuracion = repositorio.insertar(Servicio.nuevo("Consulta", null, 5000, null));
        Servicio conDuracion = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));

        assertNull(repositorio.buscarPorId(sinDuracion.id()).orElseThrow().duracionMin());
        assertEquals(45, repositorio.buscarPorId(conDuracion.id()).orElseThrow().duracionMin().intValue());
    }

    @Test
    void debeBuscarPorNombreSinDistinguirMayusculas() throws SQLException {
        Servicio guardado = repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));

        Servicio encontrado = repositorio.buscarPorNombre("baño").orElseThrow();

        assertEquals(guardado.id(), encontrado.id());
    }

    @Test
    void debeDevolverVacioAlBuscarUnServicioInexistente() throws SQLException {
        assertTrue(repositorio.buscarPorId(12345).isEmpty());
        assertTrue(repositorio.buscarPorNombre("inexistente").isEmpty());
    }

    @Test
    void debeLanzarSqlExceptionConNombreDuplicadoExacto() throws SQLException {
        repositorio.insertar(Servicio.nuevo("Baño", null, 15050, 45));

        assertThrows(SQLException.class,
                () -> repositorio.insertar(Servicio.nuevo("Baño", null, 20000, 60)));
    }
}
