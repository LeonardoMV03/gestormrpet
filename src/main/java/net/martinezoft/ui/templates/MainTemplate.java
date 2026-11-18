package net.martinezoft.ui.templates;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.organisms.SidebarNav;

/**
 * Layout principal: banner de aviso arriba, menú a la izquierda y slot de
 * contenido en el centro.
 */
public class MainTemplate extends BorderPane {

    @FXML
    private VBox bannerAviso;
    @FXML
    private Label textoAviso;
    @FXML
    private SidebarNav sidebar;
    @FXML
    private StackPane contenido;

    private final StringProperty aviso = new SimpleStringProperty("");
    private final BooleanProperty avisoVisible = new SimpleBooleanProperty(false);

    public MainTemplate() {
        CargadorFxml.cargar(this, MainTemplate.class);
        textoAviso.textProperty().bind(aviso);
        bannerAviso.visibleProperty().bind(avisoVisible);
        bannerAviso.managedProperty().bind(avisoVisible);
    }

    public SidebarNav getSidebar() {
        return sidebar;
    }

    public void setContenido(Node nodo) {
        contenido.getChildren().setAll(nodo);
    }

    public StringProperty avisoProperty() {
        return aviso;
    }

    public BooleanProperty avisoVisibleProperty() {
        return avisoVisible;
    }
}
