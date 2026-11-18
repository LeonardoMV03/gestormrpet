package net.martinezoft.ui.atoms;

import javafx.scene.control.PasswordField;

/**
 * Campo de contraseña base de la aplicación. Sin lógica.
 */
public class AppPasswordField extends PasswordField {

    public AppPasswordField() {
        getStyleClass().add("app-password-field");
    }
}
