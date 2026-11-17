package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {

    @TempDir
    Path tempDir;

    @Test
    void debeConstruirUrlJdbcConRutaAbsoluta() {
        Path archivo = tempDir.resolve("url.db");

        Database database = new Database(archivo);

        assertEquals("jdbc:sqlite:" + archivo.toAbsolutePath().normalize(), database.url());
    }

    @Test
    void debeAplicarPragmasAlAbrirConexion() throws SQLException {
        Database database = new Database(tempDir.resolve("pragmas.db"));

        try (Connection conexion = database.conexion(); Statement sentencia = conexion.createStatement()) {
            assertEquals(1, leerEntero(sentencia, "PRAGMA foreign_keys"));
            assertEquals("wal", leerTexto(sentencia, "PRAGMA journal_mode"));
            assertEquals(5000, leerEntero(sentencia, "PRAGMA busy_timeout"));
        }
    }

    @Test
    void debeImpedirFilasHijasSinClientePorForeignKeys() throws SQLException {
        Database database = new Database(tempDir.resolve("fk.db"));
        new DatabaseInitializer(database).migrar();

        try (Connection conexion = database.conexion(); Statement sentencia = conexion.createStatement()) {
            SQLException fallo = assertThrows(SQLException.class, () -> sentencia.executeUpdate(
                    "INSERT INTO mascotas (cliente_id, nombre) VALUES (999, 'Huerfana')"));
            assertTrue(fallo.getMessage().toUpperCase().contains("FOREIGN KEY"),
                    "El fallo debe deberse a la FK, no a otra restricción: " + fallo.getMessage());
        }
    }

    @Test
    void debeRechazarUrlVacia() {
        assertThrows(IllegalArgumentException.class, () -> Database.deUrl("  "));
    }

    @Test
    void debeAceptarUrlExplicita() throws SQLException {
        Path archivo = tempDir.resolve("url-explicita.db");

        Database database = Database.deUrl("jdbc:sqlite:" + archivo.toAbsolutePath());

        try (Connection conexion = database.conexion()) {
            assertTrue(conexion.isValid(2));
        }
    }

    private static int leerEntero(Statement sentencia, String pragma) throws SQLException {
        try (ResultSet filas = sentencia.executeQuery(pragma)) {
            assertTrue(filas.next());
            return filas.getInt(1);
        }
    }

    private static String leerTexto(Statement sentencia, String pragma) throws SQLException {
        try (ResultSet filas = sentencia.executeQuery(pragma)) {
            assertTrue(filas.next());
            return filas.getString(1).toLowerCase();
        }
    }
}
