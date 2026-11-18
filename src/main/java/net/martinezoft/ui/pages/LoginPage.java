package net.martinezoft.ui.pages;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.components.LoginForm;
import net.martinezoft.ui.templates.LoginTemplate;
import net.martinezoft.ui.viewmodels.LoginViewModel;

/**
 * Pantalla de acceso. Compone la plantilla de login con el formulario enlazado
 * a su ViewModel y avisa cuando la autenticación tiene éxito.
 */
public class LoginPage extends StackPane {

    @FXML
    private LoginTemplate plantilla;

    private final Runnable alAutenticar;

    public LoginPage(LoginViewModel vm, Runnable alAutenticar) {
        Objects.requireNonNull(vm, "El ViewModel de login es obligatorio");
        this.alAutenticar = Objects.requireNonNull(alAutenticar, "La acción de autenticación es obligatoria");
        CargadorFxml.cargar(this, LoginPage.class);
        plantilla.setContenido(new LoginForm(vm));
        vm.autenticadoProperty().addListener((observable, anterior, autenticado) -> {
            if (autenticado) {
                this.alAutenticar.run();
            }
        });
    }
}
