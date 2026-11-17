package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigracionesTest {

    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    void prepararBaseDeDatos() {
        database = new Database(tempDir.resolve("migraciones.db"));
        new DatabaseInitializer(database).migrar();
    }

    @Test
    void debeRegistrarLasVersionesV1V2YV3() throws SQLException {
        assertEquals(List.of("1", "2", "3"), versionesAplicadas());
    }

    @Test
    void debeCrearLasTablasDelDominio() throws SQLException {
        Set<String> tablas = tablasDeUsuario();

        assertTrue(tablas.containsAll(Set.of(
                "clientes", "mascotas", "servicios", "citas", "cita_servicios", "notas_historial", "usuarios")),
                "Faltan tablas del dominio. Encontradas: " + tablas);
    }

    @Test
    void debeAgregarLasColumnasDePagoEnCitas() throws SQLException {
        Set<String> columnas = columnasDe("citas");

        assertTrue(columnas.containsAll(Set.of("estado_pago", "metodo_pago", "pagado_en")),
                "Faltan columnas de pago. Encontradas: " + columnas);
    }

    @Test
    void debeSerIdempotenteAlMigrarDosVeces() throws SQLException {
        new DatabaseInitializer(database).migrar();

        assertEquals(List.of("1", "2", "3"), versionesAplicadas());
    }

    private List<String> versionesAplicadas() throws SQLException {
        String sql = "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            var versiones = new java.util.ArrayList<String>();
            while (filas.next()) {
                versiones.add(filas.getString("version"));
            }
            return versiones;
        }
    }

    private Set<String> tablasDeUsuario() throws SQLException {
        String sql = """
                SELECT name FROM sqlite_master
                 WHERE type = 'table'
                   AND name NOT LIKE 'sqlite_%'
                   AND name <> 'flyway_schema_history'
                """;
        Set<String> tablas = new LinkedHashSet<>();
        try (Connection conexion = database.conexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery(sql)) {
            while (filas.next()) {
                tablas.add(filas.getString("name"));
            }
        }
        return tablas;
    }

    private Set<String> columnasDe(String tabla) throws SQLException {
        Set<String> columnas = new LinkedHashSet<>();
        try (Connection conexion = database.conexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (filas.next()) {
                columnas.add(filas.getString("name"));
            }
        }
        return columnas;
    }
}
