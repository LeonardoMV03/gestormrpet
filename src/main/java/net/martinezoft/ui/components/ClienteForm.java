package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.ClientesViewModel;

/**
 * Formulario de alta y edición de clientes. Enlaza sus campos con el ViewModel
 * y delega en él cualquier regla; la baja se confirma en la página.
 */
public class ClienteForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    public ClienteForm(ClientesViewModel vm, Runnable alDesactivar) {
        Objects.requireNonNull(vm, "El ViewModel de clientes es obligatorio");
        Objects.requireNonNull(alDesactivar, "La acción de desactivar es obligatoria");
        CargadorFxml.cargar(this, ClienteForm.class);

        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Ana Pérez");
        TextField campoTelefono = new TextField();
        campoTelefono.setPromptText("555-1234");
        TextField campoEmail = new TextField();
        campoEmail.setPromptText("ana@example.com");
        TextField campoDireccion = new TextField();
        campoDireccion.setPromptText("Calle, número, colonia");
        TextField campoNotas = new TextField();
        campoNotas.setPromptText("Notas del cliente");

        campoNombre.textProperty().bindBidirectional(vm.nombreProperty());
        campoTelefono.textProperty().bindBidirectional(vm.telefonoProperty());
        campoEmail.textProperty().bindBidirectional(vm.emailProperty());
        campoDireccion.textProperty().bindBidirectional(vm.direccionProperty());
        campoNotas.textProperty().bindBidirectional(vm.notasProperty());

        FormField formNombre = new FormField("Nombre", campoNombre);
        formNombre.errorProperty().bind(vm.errorNombreProperty());
        FormField formTelefono = new FormField("Teléfono", campoTelefono);
        FormField formEmail = new FormField("Email", campoEmail);
        FormField formDireccion = new FormField("Dirección", campoDireccion);
        FormField formNotas = new FormField("Notas", campoNotas);

        campos.getChildren().setAll(formNombre, formTelefono, formEmail, formDireccion, formNotas);

        Button botonNuevo = new Button("Nuevo");
        Button botonGuardar = new Button("Guardar");
        botonGuardar.getStyleClass().add("boton-primario");
        Button botonDesactivar = new Button("Desactivar");
        botonDesactivar.getStyleClass().add("boton-peligro");

        botonGuardar.disableProperty().bind(vm.puedeGuardarProperty().not());
        botonDesactivar.disableProperty().bind(vm.seleccionadoProperty().isNull());
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
