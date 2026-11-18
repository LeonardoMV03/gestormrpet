package net.martinezoft.ui.pages;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.organisms.LoginForm;
import net.martinezoft.ui.templates.LoginTemplate;
import net.martinezoft.ui.viewmodels.LoginViewModel;

/**
 * Pantalla de acceso. Compone la plantilla de login con el formulario enlazado
 * a su ViewModel y avisa cuando la autenticación tiene éxito.
 */
public class LoginPage extends StackPane {

    @FXML
    private LoginTemplate plantilla;

    private Runnable alAutenticar = () -> { };

    public LoginPage(LoginViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de login es obligatorio");
        CargadorFxml.cargar(this, LoginPage.class);
        plantilla.setContenido(new LoginForm(vm));
        vm.autenticadoProperty().addListener((observable, anterior, autenticado) -> {
            if (autenticado) {
                alAutenticar.run();
            }
        });
    }

    public void setOnAutenticado(Runnable accion) {
        this.alAutenticar = Objects.requireNonNull(accion, "La acción es obligatoria");
    }
}
