package net.martinezoft.ui.templates;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import net.martinezoft.ui.CargadorFxml;

/**
 * Layout de acceso: tarjeta centrada con un slot de contenido.
 */
public class LoginTemplate extends StackPane {

    @FXML
    private StackPane contenido;

    public LoginTemplate() {
        CargadorFxml.cargar(this, LoginTemplate.class);
    }

    public void setContenido(Node nodo) {
        contenido.getChildren().setAll(nodo);
    }
}
