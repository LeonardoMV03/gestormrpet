package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.MascotasViewModel;

/**
 * Formulario de alta y edición de mascotas del cliente seleccionado.
 */
public class MascotaForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    public MascotaForm(MascotasViewModel vm, Runnable alDesactivar) {
        Objects.requireNonNull(vm, "El ViewModel de mascotas es obligatorio");
        Objects.requireNonNull(alDesactivar, "La acción de desactivar es obligatoria");
        CargadorFxml.cargar(this, MascotaForm.class);

        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Firulais");
        TextField campoEspecie = new TextField();
        campoEspecie.setPromptText("perro");
        TextField campoRaza = new TextField();
        campoRaza.setPromptText("Labrador");
        TextField campoSexo = new TextField();
        campoSexo.setPromptText("macho o hembra");
        TextField campoFecha = new TextField();
        campoFecha.setPromptText("AAAA-MM-DD");
        TextField campoNotas = new TextField();
        campoNotas.setPromptText("Notas de la mascota");

        campoNombre.textProperty().bindBidirectional(vm.nombreProperty());
        campoEspecie.textProperty().bindBidirectional(vm.especieProperty());
        campoRaza.textProperty().bindBidirectional(vm.razaProperty());
        campoSexo.textProperty().bindBidirectional(vm.sexoProperty());
        campoFecha.textProperty().bindBidirectional(vm.fechaNacimientoProperty());
        campoNotas.textProperty().bindBidirectional(vm.notasProperty());

        FormField formNombre = new FormField("Nombre", campoNombre);
        formNombre.errorProperty().bind(vm.errorNombreProperty());
        FormField formEspecie = new FormField("Especie", campoEspecie);
        FormField formRaza = new FormField("Raza", campoRaza);
        FormField formSexo = new FormField("Sexo", campoSexo);
        FormField formFecha = new FormField("Nacimiento", campoFecha);
        formFecha.errorProperty().bind(vm.errorFechaProperty());
        FormField formNotas = new FormField("Notas", campoNotas);

        campos.getChildren().setAll(formNombre, formEspecie, formRaza, formSexo, formFecha, formNotas);

        Button botonNuevo = new Button("Nueva");
        Button botonGuardar = new Button("Guardar");
        botonGuardar.getStyleClass().add("boton-primario");
        Button botonDesactivar = new Button("Desactivar");
        botonDesactivar.getStyleClass().add("boton-peligro");

        botonGuardar.disableProperty().bind(vm.puedeGuardarProperty().not());
        botonDesactivar.disableProperty().bind(vm.seleccionadaProperty().isNull());
        botonNuevo.setOnAction(e -> vm.nuevo());
        botonGuardar.setOnAction(e -> vm.guardar());
        botonDesactivar.setOnAction(e -> alDesactivar.run());
        zonaAccion.getChildren().setAll(botonNuevo, botonDesactivar, botonGuardar);

        etiquetaMensaje.textProperty().bind(vm.mensajeProperty());
        etiquetaMensaje.visibleProperty().bind(vm.mensajeProperty().isNotEmpty());
        etiquetaMensaje.managedProperty().bind(etiquetaMensaje.visibleProperty());

        etiquetaError.textProperty().bind(vm.errorGeneralProperty());
        etiquetaError.visibleProperty().bind(vm.errorGeneralProperty().isNotEmpty());
        etiquetaError.managedProperty().bind(etiquetaError.visibleProperty());
    }
}
