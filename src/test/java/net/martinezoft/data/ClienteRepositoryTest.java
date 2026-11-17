package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Cliente;

class ClienteRepositoryTest {

    @TempDir
    Path tempDir;

    private ClienteRepository repositorio;

    @BeforeEach
    void prepararRepositorio() {
        Database database = new Database(tempDir.resolve("clientes.db"));
        new DatabaseInitializer(database).migrar();
        repositorio = new ClienteRepository(database);
    }

    @Test
    void debeInsertarYBuscarCliente() throws SQLException {
        Cliente guardado = repositorio.insertar(
                Cliente.nuevo("Ana Pérez", "555-1234", "ana@example.com", "Calle 1", "Cliente frecuente"));

        assertNotNull(guardado.id(), "El repositorio debe devolver el id generado");
        Cliente encontrado = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertEquals(guardado, encontrado);
        assertTrue(encontrado.activo());
    }

    @Test
    void debeListarSoloClientesActivosOrdenadosPorNombre() throws SQLException {
        Cliente zoe = repositorio.insertar(Cliente.nuevo("Zoe", null, null, null, null));
        Cliente ana = repositorio.insertar(Cliente.nuevo("Ana", null, null, null, null));

        repositorio.desactivar(zoe.id());

        List<Cliente> activos = repositorio.listar();
        assertEquals(List.of(ana), activos);
    }

    @Test
    void debeActualizarLosDatosEditables() throws SQLException {
        Cliente guardado = repositorio.insertar(Cliente.nuevo("Ana", "555-0000", null, null, null));
        Cliente editado = new Cliente(guardado.id(), "Ana Pérez", "555-9999",
                "ana@example.com", "Calle 2", "Notas nuevas", true);

        assertTrue(repositorio.actualizar(editado));

        assertEquals(editado, repositorio.buscarPorId(guardado.id()).orElseThrow());
    }

    @Test
    void debeDesactivarConSoftDelete() throws SQLException {
        Cliente guardado = repositorio.insertar(Cliente.nuevo("Ana", null, null, null, null));

        assertTrue(repositorio.desactivar(guardado.id()));
        assertFalse(repositorio.desactivar(guardado.id()), "Desactivar dos veces no debe afectar filas");

        Cliente inactivo = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertFalse(inactivo.activo());
        assertFalse(repositorio.listar().contains(inactivo));
    }

    @Test
    void debeRechazarNombreVacioEnElDominio() {
        assertThrows(IllegalArgumentException.class,
                () -> Cliente.nuevo("   ", null, null, null, null));
    }
}
