package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigracionesTest {

    private static final Pattern ISO_8601_UTC =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");

    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    void prepararBaseDeDatos() {
        database = new Database(tempDir.resolve("migraciones.db"));
        new DatabaseInitializer(database).migrar();
    }

    @Test
    void debeRegistrarLasVersionesV1V2V3V4YV5() throws SQLException {
        assertEquals(List.of("1", "2", "3", "4", "5"), versionesAplicadas());
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
    void debeCrearLaTablaUsuariosConSusColumnas() throws SQLException {
        Set<String> columnas = columnasDe("usuarios");

        assertTrue(columnas.containsAll(Set.of("id", "usuario", "clave_hash", "creado_en", "actualizado_en")),
                "Faltan columnas en usuarios. Encontradas: " + columnas);
    }

    @Test
    void debeAgregarElAvisoDeCambioDePassword() throws SQLException {
        Set<String> columnas = columnasDe("usuarios");

        assertTrue(columnas.contains("debe_cambiar_password"),
                "Falta la columna debe_cambiar_password. Encontradas: " + columnas);
    }

    @Test
    void debeInicializarDebeCambiarPasswordEnCero() throws SQLException {
        try (Connection conexion = database.conexion(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO usuarios (usuario, clave_hash) VALUES ('admin', 'hash')");

            try (ResultSet filas = sentencia.executeQuery(
                    "SELECT debe_cambiar_password FROM usuarios WHERE usuario = 'admin'")) {
                assertTrue(filas.next());
                assertEquals(0, filas.getInt("debe_cambiar_password"),
                        "El valor por defecto de debe_cambiar_password debe ser 0");
            }
        }
    }

    @Test
    void debeCrearLasColumnasBaseDeClientes() throws SQLException {
        Set<String> columnas = columnasDe("clientes");

        assertTrue(columnas.containsAll(Set.of(
                "id", "nombre", "telefono", "email", "direccion", "notas", "activo", "creado_en", "actualizado_en")),
                "Faltan columnas en clientes. Encontradas: " + columnas);
    }

    @Test
    void debeCrearLosIndicesDelEsquemaBase() throws SQLException {
        Set<String> indices = indicesDeUsuario();

        assertTrue(indices.containsAll(Set.of("idx_mascotas_cliente", "idx_citas_mascota", "idx_notas_mascota",
                        "idx_citas_fecha_hora", "idx_citas_pago")),
                "Faltan índices del esquema base. Encontrados: " + indices);
    }

    @Test
    void debeRechazarElEstadoConfirmadaEnLaBaseDeDatos() throws SQLException {
        try (Connection conexion = database.conexion(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO clientes (nombre) VALUES ('Ana')");
            sentencia.executeUpdate("INSERT INTO mascotas (cliente_id, nombre) VALUES (1, 'Luna')");

            assertThrows(SQLException.class, () -> sentencia.executeUpdate(
                    "INSERT INTO citas (mascota_id, fecha_hora, estado) VALUES (1, '2026-11-17T10:00:00', 'confirmada')"));
        }
    }

    @Test
    void debeConservarDatosAlMigrarV5() throws SQLException {
        Path archivo = tempDir.resolve("datos-v5.db");
        Database anterior = new Database(archivo);
        Flyway.configure()
                .dataSource(anterior.url(), "", "")
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate();

        try (Connection conexion = anterior.conexion(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO clientes (nombre) VALUES ('Ana')");
            sentencia.executeUpdate("INSERT INTO mascotas (cliente_id, nombre) VALUES (1, 'Luna')");
            sentencia.executeUpdate(
                    "INSERT INTO servicios (nombre, precio_centavos) VALUES ('Baño', 15050)");
            sentencia.executeUpdate("""
                    INSERT INTO citas (id, mascota_id, fecha_hora, estado)
                    VALUES (10, 1, '2026-11-17T10:00:00', 'confirmada')
                    """);
            sentencia.executeUpdate(
                    "INSERT INTO cita_servicios (cita_id, servicio_id, precio_centavos) VALUES (10, 1, 15050)");
            sentencia.executeUpdate("""
                    INSERT INTO notas_historial (mascota_id, cita_id, fecha, texto)
                    VALUES (1, 10, '2026-11-17T10:00:00', 'Nota anterior')
                    """);
        }

        new DatabaseInitializer(anterior).migrar();

        try (Connection conexion = anterior.conexion(); Statement sentencia = conexion.createStatement()) {
            try (ResultSet cita = sentencia.executeQuery("SELECT estado FROM citas WHERE id = 10")) {
                assertTrue(cita.next());
                assertEquals("pendiente", cita.getString("estado"));
            }
            try (ResultSet servicios = sentencia.executeQuery(
                    "SELECT COUNT(*) FROM cita_servicios WHERE cita_id = 10")) {
                assertTrue(servicios.next());
                assertEquals(1, servicios.getInt(1));
            }
            try (ResultSet notas = sentencia.executeQuery(
                    "SELECT COUNT(*) FROM notas_historial WHERE cita_id = 10")) {
                assertTrue(notas.next());
                assertEquals(1, notas.getInt(1));
            }
        }
    }

    @Test
    void debeSerIdempotenteAlMigrarDosVeces() throws SQLException {
        new DatabaseInitializer(database).migrar();

        assertEquals(List.of("1", "2", "3", "4", "5"), versionesAplicadas());
    }

    @Test
    void debeUsarTimestampsIso8601EnLosDefaultsDeCreadoEn() throws SQLException {
        try (Connection conexion = database.conexion(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO clientes (nombre) VALUES ('Ana')");
            sentencia.executeUpdate("INSERT INTO mascotas (cliente_id, nombre) VALUES (1, 'Luna')");
            sentencia.executeUpdate("INSERT INTO citas (mascota_id, fecha_hora) VALUES (1, '2026-11-17T10:00:00Z')");
            sentencia.executeUpdate(
                    "INSERT INTO notas_historial (mascota_id, fecha, texto) VALUES (1, '2026-11-17T10:00:00Z', 'Nota')");
            sentencia.executeUpdate("INSERT INTO usuarios (usuario, clave_hash) VALUES ('admin', 'hash')");

            for (String tabla : List.of("clientes", "mascotas", "citas", "notas_historial", "usuarios")) {
                String creadoEn = leerPrimerCreadoEn(sentencia, tabla);
                assertTrue(ISO_8601_UTC.matcher(creadoEn).matches(),
                        "creado_en de " + tabla + " debe ser ISO-8601 UTC (YYYY-MM-DDTHH:MM:SSZ): " + creadoEn);
            }
        }
    }

    private static String leerPrimerCreadoEn(Statement sentencia, String tabla) throws SQLException {
        String sql = "SELECT creado_en FROM " + tabla + " ORDER BY id LIMIT 1";
        try (ResultSet filas = sentencia.executeQuery(sql)) {
            assertTrue(filas.next(), "Debe existir al menos una fila en " + tabla);
            return filas.getString("creado_en");
        }
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

    private Set<String> indicesDeUsuario() throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'";
        Set<String> indices = new LinkedHashSet<>();
        try (Connection conexion = database.conexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery(sql)) {
            while (filas.next()) {
                indices.add(filas.getString("name"));
            }
        }
        return indices;
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
