package net.martinezoft.ui.pages;

import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.domain.Servicio;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.organisms.ServicioForm;
import net.martinezoft.ui.organisms.ServiciosTable;
import net.martinezoft.ui.viewmodels.ServiciosViewModel;

/**
 * Pantalla de servicios: tabla de activos y formulario de alta/edición. La
 * confirmación de baja es responsabilidad de la vista; la regla vive en el
 * ViewModel.
 */
public class ServiciosPage extends VBox {

    @FXML
    private StackPane slotTabla;
    @FXML
    private StackPane slotFormulario;

    private final ServiciosViewModel vm;

    public ServiciosPage(ServiciosViewModel vm) {
        this.vm = Objects.requireNonNull(vm, "El ViewModel de servicios es obligatorio");
        CargadorFxml.cargar(this, ServiciosPage.class);
        slotTabla.getChildren().setAll(new ServiciosTable(vm));
        slotFormulario.getChildren().setAll(new ServicioForm(vm, this::confirmarDesactivar));
        vm.cargar();
    }

    private void confirmarDesactivar() {
        Servicio seleccionado = vm.seleccionadoProperty().get();
        if (seleccionado == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(getScene() != null ? getScene().getWindow() : null);
        confirmacion.setTitle("Desactivar servicio");
        confirmacion.setHeaderText("¿Desactivar \"" + seleccionado.nombre() + "\"?");
        confirmacion.setContentText(
                "El servicio dejará de aparecer en la lista, pero sus datos se conservan.");
        ButtonType desactivar = new ButtonType("Desactivar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(desactivar, cancelar);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == desactivar) {
            vm.desactivarSeleccionado();
        }
    }
}
