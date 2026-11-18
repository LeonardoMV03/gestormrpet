package net.martinezoft.ui.molecules;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;

/**
 * Etiqueta, control de entrada y mensaje de error. El control se recibe
 * desde fuera; el error se enlaza a una propiedad y se oculta si está vacío.
 */
public class FormField extends VBox {

    @FXML
    private Label etiqueta;
    @FXML
    private StackPane contenedorControl;
    @FXML
    private Label mensajeError;

    private final StringProperty error = new SimpleStringProperty("");

    public FormField(String texto, Control control) {
        Objects.requireNonNull(control, "El control es obligatorio");
        CargadorFxml.cargar(this, FormField.class);
        etiqueta.setText(texto);
        etiqueta.setLabelFor(control);
        contenedorControl.getChildren().add(control);
        mensajeError.textProperty().bind(error);
        mensajeError.visibleProperty().bind(error.isNotEmpty());
        mensajeError.managedProperty().bind(mensajeError.visibleProperty());
    }

    public StringProperty errorProperty() {
        return error;
    }
}
