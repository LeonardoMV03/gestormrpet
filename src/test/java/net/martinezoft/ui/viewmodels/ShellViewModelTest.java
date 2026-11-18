package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.config.SesionManager;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.UsuarioRepository;
import net.martinezoft.domain.Usuario;
import net.martinezoft.security.PasswordHasher;
import net.martinezoft.service.AuthService;

class ShellViewModelTest {

    private static final String CLAVE_NUEVA = "clave-nueva-123";

    @TempDir
    Path tempDir;

    private RelojMutable reloj;
    private SesionManager sesion;
    private AuthService auth;

    @BeforeEach
    void prepararServicios() throws Exception {
        reloj = new RelojMutable(Instant.parse("2026-11-17T08:00:00Z"));
        sesion = new SesionManager(reloj);
        Database database = new Database(tempDir.resolve("shell.db"));
        new DatabaseInitializer(database).migrar();
        UsuarioRepository usuarios = new UsuarioRepository(database);
        auth = new AuthService(usuarios, new PasswordHasher(), sesion);
        auth.asegurarAdmin();
    }

    @Test
    void debeMostrarAvisoSiDebeCambiarPassword() throws Exception {
        auth.iniciarSesion("admin", "admin");

        ShellViewModel shell = new ShellViewModel(sesion);

        assertTrue(shell.avisoClaveVisibleProperty().get());
        assertEquals(ShellViewModel.MENSAJE_AVISO_CLAVE, shell.avisoClaveProperty().get());
        assertEquals("admin", shell.usuarioActualProperty().get());
    }

    @Test
    void noDebeMostrarAvisoTrasCambiarLaClave() throws Exception {
        Usuario admin = auth.iniciarSesion("admin", "admin").orElseThrow().usuario();
        auth.cambiarClave(admin.id(), "admin", CLAVE_NUEVA);
        sesion.cerrar();
        auth.iniciarSesion("admin", CLAVE_NUEVA);

        ShellViewModel shell = new ShellViewModel(sesion);

        assertFalse(shell.avisoClaveVisibleProperty().get());
        assertEquals("", shell.avisoClaveProperty().get());
    }

    @Test
    void sinSesionNoHayAvisoNiSesionVigente() {
        ShellViewModel shell = new ShellViewModel(sesion);

        assertFalse(shell.avisoClaveVisibleProperty().get());
        assertFalse(shell.sesionVigente());
        assertEquals("", shell.usuarioActualProperty().get());
    }

    @Test
    void cerrarSesionDejaSesionInactiva() throws Exception {
        auth.iniciarSesion("admin", "admin");
        ShellViewModel shell = new ShellViewModel(sesion);

        shell.cerrarSesion();

        assertFalse(sesion.activa());
        assertFalse(shell.sesionVigente());
    }

    @Test
    void sesionCaducaALas12Horas() throws Exception {
        auth.iniciarSesion("admin", "admin");
        ShellViewModel shell = new ShellViewModel(sesion);

        reloj.avanzar(SesionManager.DURACION.minusSeconds(1));
        assertTrue(shell.sesionVigente(), "Justo antes de 12 h la sesión sigue vigente");

        reloj.avanzar(Duration.ofSeconds(1));
        assertFalse(shell.sesionVigente(), "A las 12 h la sesión debe caducar");
    }
}
