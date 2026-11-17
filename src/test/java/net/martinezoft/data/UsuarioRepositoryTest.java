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
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Usuario;

class UsuarioRepositoryTest {

    private static final Pattern ISO_8601_UTC =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");

    @TempDir
    Path tempDir;

    private Database database;
    private UsuarioRepository repositorio;

    @BeforeEach
    void prepararRepositorio() {
        database = new Database(tempDir.resolve("usuarios.db"));
        new DatabaseInitializer(database).migrar();
        repositorio = new UsuarioRepository(database);
    }

    @Test
    void debeInsertarYBuscarPorUsuario() throws SQLException {
        Usuario guardado = repositorio.insertar("admin", "hash-inicial", true);

        assertNotNull(guardado.id(), "El repositorio debe devolver el id generado");
        assertEquals("admin", guardado.usuario());
        assertTrue(guardado.debeCambiarPassword());

        assertEquals(guardado, repositorio.buscarPorUsuario("admin").orElseThrow());
    }

    @Test
    void debeDevolverVacioAlBuscarUnUsuarioInexistente() throws SQLException {
        assertTrue(repositorio.buscarPorUsuario("nadie").isEmpty());
        assertTrue(repositorio.buscarPorId(12345).isEmpty());
    }

    @Test
    void debeContarUsuarios() throws SQLException {
        assertEquals(0, repositorio.contar());

        repositorio.insertar("admin", "hash-inicial", true);

        assertEquals(1, repositorio.contar());
    }

    @Test
    void debeActualizarClaveYFlag() throws SQLException {
        Usuario guardado = repositorio.insertar("admin", "hash-inicial", true);

        assertTrue(repositorio.actualizarClave(guardado.id(), "hash-nuevo", false));

        Usuario actualizado = repositorio.buscarPorId(guardado.id()).orElseThrow();
        assertEquals("hash-nuevo", actualizado.claveHash());
        assertFalse(actualizado.debeCambiarPassword());
        assertIso8601(leerTimestamp(guardado.id(), "actualizado_en"));
    }

    @Test
    void noDebeActualizarLaClaveDeUnUsuarioInexistente() throws SQLException {
        assertFalse(repositorio.actualizarClave(999, "hash-nuevo", false));
    }

    @Test
    void noDebePermitirUsuariosDuplicados() throws SQLException {
        repositorio.insertar("admin", "hash-inicial", true);

        assertThrows(SQLException.class, () -> repositorio.insertar("admin", "otro-hash", true));
    }

    @Test
    void debeCrearSinAvisoDeCambioCuandoSeSolicita() throws SQLException {
        Usuario guardado = repositorio.insertar("admin", "hash-inicial", false);

        assertFalse(repositorio.buscarPorUsuario("admin").orElseThrow().debeCambiarPassword());
        assertEquals(guardado.id(), repositorio.buscarPorUsuario("admin").orElseThrow().id());
    }

    @Test
    void debeDejarActualizadoEnNuloHastaLaPrimeraModificacion() throws SQLException {
        Usuario guardado = repositorio.insertar("admin", "hash-inicial", true);

        assertNull(leerTimestamp(guardado.id(), "actualizado_en"));
    }

    private String leerTimestamp(long id, String columna) throws SQLException {
        String sql = "SELECT " + columna + " FROM usuarios WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                assertTrue(filas.next(), "El usuario debe existir para leer " + columna);
                return filas.getString(columna);
            }
        }
    }

    private static void assertIso8601(String valor) {
        assertNotNull(valor, "actualizado_en no debe ser nulo");
        assertTrue(ISO_8601_UTC.matcher(valor).matches(),
                "actualizado_en debe ser ISO-8601 UTC (YYYY-MM-DDTHH:MM:SSZ): " + valor);
    }
}
