package net.martinezoft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.config.SesionManager;
import net.martinezoft.config.SesionManager.Sesion;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.UsuarioRepository;
import net.martinezoft.domain.Usuario;
import net.martinezoft.security.PasswordHasher;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private UsuarioRepository repositorio;
    private PasswordHasher hasher;
    private SesionManager sesion;
    private AuthService auth;

    @BeforeEach
    void prepararServicio() throws SQLException {
        Database database = new Database(tempDir.resolve("auth.db"));
        new DatabaseInitializer(database).migrar();
        repositorio = new UsuarioRepository(database);
        hasher = new PasswordHasher();
        sesion = new SesionManager();
        auth = new AuthService(repositorio, hasher, sesion);
        auth.asegurarAdmin();
    }

    @Test
    void debeSembrarAdminConAvisoDeCambio() throws SQLException {
        assertEquals(1, repositorio.contar());

        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();
        assertEquals("admin", admin.usuario());
        assertTrue(admin.debeCambiarPassword(), "El admin inicial debe avisar del cambio de contraseña");
        assertNotEquals("admin", admin.claveHash(), "La contraseña no puede guardarse en texto plano");
        assertTrue(admin.claveHash().startsWith("pbkdf2$sha256$"),
                "El hash sembrado debe usar el formato PBKDF2-SHA256");
        assertFalse(admin.claveHash().contains("admin"),
                "El hash almacenado no debe contener la contraseña en claro");
        assertTrue(hasher.verificar("admin", admin.claveHash()));
    }

    @Test
    void debeSerIdempotenteAlSembrarAdmin() throws SQLException {
        auth.asegurarAdmin();
        auth.asegurarAdmin();

        assertEquals(1, repositorio.contar(), "El seed no debe duplicar el usuario admin");
    }

    @Test
    void debeIniciarSesionConCredencialesCorrectas() throws SQLException {
        Optional<Sesion> iniciada = auth.iniciarSesion("admin", "admin");

        assertTrue(iniciada.isPresent());
        assertTrue(sesion.activa());
        assertEquals("admin", sesion.actual().orElseThrow().usuario().usuario());
    }

    @Test
    void debeRechazarCredencialesIncorrectas() throws SQLException {
        assertTrue(auth.iniciarSesion("admin", "incorrecta").isEmpty());
        assertTrue(auth.iniciarSesion("desconocido", "admin").isEmpty());
        assertTrue(auth.iniciarSesion("admin", null).isEmpty());
        assertTrue(auth.iniciarSesion(null, "admin").isEmpty());
        assertFalse(sesion.activa(), "No debe quedar sesión tras un login fallido");
    }

    @Test
    void debeCambiarClaveYLimpiarElAviso() throws SQLException {
        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();

        auth.cambiarClave(admin.id(), "admin", "nuevaClave123");

        Usuario actualizado = repositorio.buscarPorId(admin.id()).orElseThrow();
        assertFalse(actualizado.debeCambiarPassword(), "El flag debe limpiarse tras el cambio");
        assertNotEquals(admin.claveHash(), actualizado.claveHash());

        assertTrue(auth.iniciarSesion("admin", "admin").isEmpty(), "La contraseña anterior ya no debe servir");
        assertTrue(auth.iniciarSesion("admin", "nuevaClave123").isPresent());
    }

    @Test
    void debeRechazarContrasenaActualIncorrecta() throws SQLException {
        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> auth.cambiarClave(admin.id(), "incorrecta", "nuevaClave123"));

        assertEquals(admin.claveHash(), repositorio.buscarPorId(admin.id()).orElseThrow().claveHash());
        assertTrue(repositorio.buscarPorId(admin.id()).orElseThrow().debeCambiarPassword());
    }

    @Test
    void debeRechazarContrasenaNuevaDemasiadoCorta() throws SQLException {
        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> auth.cambiarClave(admin.id(), "admin", "corta"));
    }

    @Test
    void debeRechazarContrasenaNuevaEnBlanco() throws SQLException {
        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> auth.cambiarClave(admin.id(), "admin", "        "));
    }

    @Test
    void debeRechazarContrasenaNuevaIgualALaActual() throws SQLException {
        Usuario admin = repositorio.buscarPorUsuario("admin").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> auth.cambiarClave(admin.id(), "admin", "admin"));
    }

    @Test
    void debeRechazarElCambioParaUnUsuarioInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.cambiarClave(999, "admin", "nuevaClave123"));
    }
}
