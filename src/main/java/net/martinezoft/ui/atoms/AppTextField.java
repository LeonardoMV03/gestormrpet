package net.martinezoft.ui.atoms;

import javafx.scene.control.TextField;

/**
 * Campo de texto base de la aplicación. Sin lógica.
 */
public class AppTextField extends TextField {

    public AppTextField() {
        getStyleClass().add("app-text-field");
    }
}
