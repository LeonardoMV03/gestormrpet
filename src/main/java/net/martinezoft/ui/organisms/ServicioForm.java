package net.martinezoft.ui.organisms;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.atoms.AppButton;
import net.martinezoft.ui.atoms.AppTextField;
import net.martinezoft.ui.molecules.FormField;
import net.martinezoft.ui.viewmodels.ServiciosViewModel;

/**
 * Formulario de alta y edición de servicios. Enlaza sus campos con el
 * ViewModel y delega en él cualquier regla; el botón de baja avisa a la página
 * para que confirme antes de ejecutar.
 */
public class ServicioForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    private final AppButton botonNuevo = new AppButton("Nuevo", AppButton.Variante.SECUNDARIA);
    private final AppButton botonGuardar = new AppButton("Guardar", AppButton.Variante.PRIMARIA);
    private final AppButton botonDesactivar = new AppButton("Desactivar", AppButton.Variante.PELIGRO);

    public ServicioForm(ServiciosViewModel vm, Runnable alDesactivar) {
        Objects.requireNonNull(vm, "El ViewModel de servicios es obligatorio");
        Objects.requireNonNull(alDesactivar, "La acción de desactivar es obligatoria");
        CargadorFxml.cargar(this, ServicioForm.class);

        AppTextField campoNombre = new AppTextField();
        campoNombre.setPromptText("Baño");
        AppTextField campoDescripcion = new AppTextField();
        campoDescripcion.setPromptText("Descripción breve");
        AppTextField campoPrecio = new AppTextField();
        campoPrecio.setPromptText("150,00");
        AppTextField campoDuracion = new AppTextField();
        campoDuracion.setPromptText("45");

        campoNombre.textProperty().bindBidirectional(vm.nombreProperty());
        campoDescripcion.textProperty().bindBidirectional(vm.descripcionProperty());
        campoPrecio.textProperty().bindBidirectional(vm.precioTextoProperty());
        campoDuracion.textProperty().bindBidirectional(vm.duracionTextoProperty());

        FormField formNombre = new FormField("Nombre", campoNombre);
        formNombre.errorProperty().bind(vm.errorNombreProperty());
        FormField formDescripcion = new FormField("Descripción", campoDescripcion);
        FormField formPrecio = new FormField("Precio", campoPrecio);
        formPrecio.errorProperty().bind(vm.errorPrecioProperty());
        FormField formDuracion = new FormField("Duración (min)", campoDuracion);
        formDuracion.errorProperty().bind(vm.errorDuracionProperty());

        campos.getChildren().setAll(formNombre, formDescripcion, formPrecio, formDuracion);

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
