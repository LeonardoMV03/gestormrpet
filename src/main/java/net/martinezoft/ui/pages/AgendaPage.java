package net.martinezoft.ui.pages;

import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.components.CitaAcciones;
import net.martinezoft.ui.components.CitaForm;
import net.martinezoft.ui.components.CitasTable;
import net.martinezoft.ui.viewmodels.AgendaViewModel;

/**
 * Agenda de visitas: hoy y próximas, alta de visitas y acciones sobre la
 * visita seleccionada. La confirmación de cancelar vive en la vista.
 */
public class AgendaPage extends VBox {

    @FXML
    private StackPane slotHoy;
    @FXML
    private StackPane slotProximas;
    @FXML
    private VBox panelLateral;

    private final AgendaViewModel vm;

    public AgendaPage(AgendaViewModel vm) {
        this.vm = Objects.requireNonNull(vm, "El ViewModel de agenda es obligatorio");
        CargadorFxml.cargar(this, AgendaPage.class);

        slotHoy.getChildren().setAll(new CitasTable(vm.citasHoy(), vm.seleccionadaProperty(),
                "No hay visitas para hoy"));
        slotProximas.getChildren().setAll(new CitasTable(vm.citasProximas(), vm.seleccionadaProperty(),
                "No hay visitas próximas"));
        panelLateral.getChildren().setAll(new CitaForm(vm),
                new CitaAcciones(vm, this::confirmarCancelar));
        vm.cargar();
    }

    public void recargar() {
        vm.cargar();
    }

    private void confirmarCancelar() {
        CitaDetalle seleccionada = vm.seleccionadaProperty().get();
        if (seleccionada == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(getScene() != null ? getScene().getWindow() : null);
        confirmacion.setTitle("Cancelar visita");
        confirmacion.setHeaderText("¿Cancelar la visita de \"" + seleccionada.mascotaNombre() + "\"?");
        confirmacion.setContentText("La visita quedará cancelada y ya no contará para la agenda.");
        ButtonType cancelarVisita = new ButtonType("Cancelar visita", ButtonBar.ButtonData.OK_DONE);
        ButtonType volver = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(cancelarVisita, volver);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == cancelarVisita) {
            vm.cancelarSeleccionada();
        }
    }
}
