package net.martinezoft.ui.pages;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.organisms.LoginForm;
import net.martinezoft.ui.templates.LoginTemplate;

/**
 * Pantalla de acceso. Compone la plantilla de login con el formulario y
 * expone este último para que el ViewModel lo enlace.
 */
public class LoginPage extends StackPane {

    @FXML
    private LoginTemplate plantilla;

    private final LoginForm formulario = new LoginForm();

    public LoginPage() {
        CargadorFxml.cargar(this, LoginPage.class);
        plantilla.setContenido(formulario);
    }

    public LoginForm getFormulario() {
        return formulario;
    }
}
