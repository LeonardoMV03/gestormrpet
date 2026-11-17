package net.martinezoft.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Usuario;

class AppContextTest {

    @TempDir
    Path tempDir;

    @Test
    void debeMigrarYSembrarAdminEnLaBaseTemporal() throws SQLException {
        Path archivo = tempDir.resolve("contexto.db");
        AppContext contexto = AppContext.crear(archivo);

        contexto.inicializarBaseDatos();

        Path rutaAbsoluta = archivo.toAbsolutePath().normalize();
        assertTrue(Files.exists(archivo), "La base de datos temporal debe crearse en la ruta indicada");
        assertTrue(contexto.database().url().contains(rutaAbsoluta.toString()),
                "El contexto debe apuntar solo a la base de datos temporal: " + contexto.database().url());
        assertEquals(List.of("1", "2", "3", "4"), versionesAplicadas(contexto),
                "AppContext debe aplicar las migraciones V1-V4");
        assertEquals(1, contexto.usuarios().contar(), "Debe sembrarse exactamente un usuario admin");

        Usuario admin = contexto.usuarios().buscarPorUsuario("admin").orElseThrow();
        assertTrue(admin.debeCambiarPassword(), "El admin sembrado debe avisar del cambio de contraseña");
        assertTrue(admin.claveHash().startsWith("pbkdf2$sha256$"),
                "El hash sembrado debe usar PBKDF2-SHA256");
        assertTrue(contexto.hasher().verificar("admin", admin.claveHash()));

        assertTrue(contexto.auth().iniciarSesion("admin", "admin").isPresent());
        assertTrue(contexto.sesion().activa(), "El login debe dejar una sesión activa en el contexto");
    }

    @Test
    void debeSerIdempotenteAlInicializarDosVeces() throws SQLException {
        AppContext contexto = AppContext.crear(tempDir.resolve("idempotente.db"));

        contexto.inicializarBaseDatos();
        contexto.inicializarBaseDatos();

        assertEquals(1, contexto.usuarios().contar(), "Inicializar dos veces no debe duplicar el admin");
        assertEquals(List.of("1", "2", "3", "4"), versionesAplicadas(contexto));
    }

    @Test
    void debeAceptarUnaBaseDeDatosYaConstruida() {
        AppContext contexto = AppContext.crear(new net.martinezoft.data.Database(tempDir.resolve("existente.db")));

        assertNotNull(contexto.database());
        assertNotNull(contexto.usuarios());
        assertNotNull(contexto.hasher());
        assertNotNull(contexto.sesion());
        assertNotNull(contexto.auth());
    }

    private static List<String> versionesAplicadas(AppContext contexto) throws SQLException {
        String sql = "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank";
        try (Connection conexion = contexto.database().conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            List<String> versiones = new ArrayList<>();
            while (filas.next()) {
                versiones.add(filas.getString("version"));
            }
            return versiones;
        }
    }
}
