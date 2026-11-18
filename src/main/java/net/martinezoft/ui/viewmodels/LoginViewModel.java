package net.martinezoft.ui.viewmodels;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.martinezoft.config.SesionManager.Sesion;
import net.martinezoft.service.AuthService;

/**
 * Estado y comandos de la pantalla de acceso. No conoce vistas: la navegación
 * observa {@link #autenticadoProperty()} y decide qué mostrar.
 */
public class LoginViewModel {

    static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo iniciar sesión, intenta de nuevo";

    private final AuthService auth;
    private final StringProperty usuario = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty autenticado = new SimpleBooleanProperty(false);
    private final BooleanBinding puedeIniciar;

    public LoginViewModel(AuthService auth) {
        this.auth = Objects.requireNonNull(auth, "El servicio de autenticación es obligatorio");
        this.puedeIniciar = Bindings.createBooleanBinding(
                () -> !usuario.get().isBlank() && !password.get().isEmpty(),
                usuario, password);
    }

    /**
     * Valida las credenciales. Ante fallo muestra un mensaje genérico y borra la
     * contraseña; nunca expone detalles de la excepción.
     */
    public void iniciarSesion() {
        try {
            Optional<Sesion> sesion = auth.iniciarSesion(usuario.get(), password.get());
            if (sesion.isPresent()) {
                password.set("");
                error.set("");
                autenticado.set(true);
            } else {
                fallar(MENSAJE_CREDENCIALES);
            }
        } catch (SQLException e) {
            fallar(MENSAJE_ERROR_TECNICO);
        }
    }

    private void fallar(String mensaje) {
        password.set("");
        error.set(mensaje);
    }

    public StringProperty usuarioProperty() {
        return usuario;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public BooleanBinding puedeIniciarProperty() {
        return puedeIniciar;
    }

    public BooleanProperty autenticadoProperty() {
        return autenticado;
    }
}
