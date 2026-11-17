package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Cliente;

class ClienteRepositoryTest {

    private static final Pattern ISO_8601_UTC =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");

    @TempDir
    Path tempDir;

    private Database database;
    private ClienteRepository repositorio;

    @BeforeEach
    void prepararRepositorio() {
        database = new Database(tempDir.resolve("clientes.db"));
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
    void debeDevolverVacioAlBuscarUnClienteInexistente() throws SQLException {
        assertTrue(repositorio.buscarPorId(12345).isEmpty());
    }

    @Test
    void noDebeActualizarUnClienteInactivo() throws SQLException {
        Cliente guardado = repositorio.insertar(Cliente.nuevo("Ana", null, null, null, null));
        repositorio.desactivar(guardado.id());

        Cliente editado = new Cliente(guardado.id(), "Ana Pérez", null, null, null, null, true);

        assertFalse(repositorio.actualizar(editado), "Un cliente inactivo no debe actualizarse");
    }

    @Test
    void debeRechazarNombreVacioEnElDominio() {
        assertThrows(IllegalArgumentException.class,
                () -> Cliente.nuevo("   ", null, null, null, null));
    }

    @Test
    void debeRegistrarCreadoEnEnFormatoIso8601() throws SQLException {
        Cliente guardado = repositorio.insertar(Cliente.nuevo("Ana", null, null, null, null));

        assertIso8601(leerTimestamp(guardado.id(), "creado_en"), "creado_en");
        assertNull(leerTimestamp(guardado.id(), "actualizado_en"),
                "actualizado_en debe ser nulo hasta la primera modificación");
    }

    @Test
    void debeRegistrarActualizadoEnEnFormatoIso8601() throws SQLException {
        Cliente guardado = repositorio.insertar(Cliente.nuevo("Ana", null, null, null, null));
        Cliente editado = new Cliente(guardado.id(), "Ana Pérez", null, null, null, null, true);

        assertTrue(repositorio.actualizar(editado));
        assertIso8601(leerTimestamp(guardado.id(), "actualizado_en"), "actualizado_en tras actualizar");

        assertTrue(repositorio.desactivar(guardado.id()));
        assertIso8601(leerTimestamp(guardado.id(), "actualizado_en"), "actualizado_en tras desactivar");
    }

    private String leerTimestamp(long id, String columna) throws SQLException {
        String sql = "SELECT " + columna + " FROM clientes WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                assertTrue(filas.next(), "El cliente debe existir para leer " + columna);
                return filas.getString(columna);
            }
        }
    }

    private static void assertIso8601(String valor, String etiqueta) {
        assertNotNull(valor, etiqueta + " no debe ser nulo");
        assertTrue(ISO_8601_UTC.matcher(valor).matches(),
                etiqueta + " debe ser ISO-8601 UTC (YYYY-MM-DDTHH:MM:SSZ): " + valor);
    }
}
