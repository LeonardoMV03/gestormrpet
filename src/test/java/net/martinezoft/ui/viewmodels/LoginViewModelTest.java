package net.martinezoft.ui.viewmodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.config.SesionManager;
import net.martinezoft.data.Database;
import net.martinezoft.data.UsuarioRepository;
import net.martinezoft.di.AppContext;
import net.martinezoft.security.PasswordHasher;
import net.martinezoft.service.AuthService;

class LoginViewModelTest {

    @TempDir
    Path tempDir;

    private LoginViewModel vm;

    @BeforeEach
    void prepararViewModel() throws Exception {
        AppContext contexto = AppContext.crear(new Database(tempDir.resolve("login.db")));
        contexto.inicializarBaseDatos();
        vm = new LoginViewModel(contexto.auth());
    }

    @Test
    void debeDeshabilitarInicioConCamposVacios() {
        assertFalse(vm.puedeIniciarProperty().get());

        vm.usuarioProperty().set("admin");
        assertFalse(vm.puedeIniciarProperty().get(), "Sin contraseña no debe permitir iniciar");

        vm.usuarioProperty().set("   ");
        vm.passwordProperty().set("admin");
        assertFalse(vm.puedeIniciarProperty().get(), "Un usuario solo con espacios no cuenta como válido");
    }

    @Test
    void debeDeshabilitarInicioConContrasenaSoloEspacios() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("   ");

        assertFalse(vm.puedeIniciarProperty().get(), "Una contraseña solo de espacios no debe habilitar el inicio");
    }

    @Test
    void debeHabilitarInicioConUsuarioYContrasenaRellenos() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("admin");

        assertTrue(vm.puedeIniciarProperty().get());
    }

    @Test
    void debeMostrarErrorGenericoConCredencialesIncorrectas() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("incorrecta");

        vm.iniciarSesion();

        assertEquals(LoginViewModel.MENSAJE_CREDENCIALES, vm.errorProperty().get());
        assertFalse(vm.autenticadoProperty().get());
    }

    @Test
    void debeLimpiarLaContrasenaTrasFallo() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("incorrecta");

        vm.iniciarSesion();

        assertEquals("", vm.passwordProperty().get());
    }

    @Test
    void debeAutenticarConCredencialesCorrectas() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("admin");

        vm.iniciarSesion();

        assertTrue(vm.autenticadoProperty().get());
    }

    @Test
    void debeLimpiarErrorTrasExito() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("incorrecta");
        vm.iniciarSesion();
        assertFalse(vm.errorProperty().get().isEmpty());

        vm.passwordProperty().set("admin");
        vm.iniciarSesion();

        assertTrue(vm.autenticadoProperty().get());
        assertEquals("", vm.errorProperty().get());
    }

    @Test
    void debeLimpiarLaContrasenaTrasExito() {
        vm.usuarioProperty().set("admin");
        vm.passwordProperty().set("admin");

        vm.iniciarSesion();

        assertTrue(vm.autenticadoProperty().get());
        assertEquals("", vm.passwordProperty().get());
    }

    @Test
    void debeMostrarMensajeTecnicoSinDetallesSiFallaLaBaseDeDatos() {
        Path inexistente = tempDir.resolve("no-existe").resolve("bd.db");
        AuthService auth = new AuthService(
                new UsuarioRepository(Database.deUrl("jdbc:sqlite:" + inexistente)),
                new PasswordHasher(),
                new SesionManager());
        LoginViewModel conFallo = new LoginViewModel(auth);
        conFallo.usuarioProperty().set("admin");
        conFallo.passwordProperty().set("admin");

        conFallo.iniciarSesion();

        assertEquals(LoginViewModel.MENSAJE_ERROR_TECNICO, conFallo.errorProperty().get());
        assertFalse(conFallo.autenticadoProperty().get());
        assertEquals("", conFallo.passwordProperty().get());
    }
}
