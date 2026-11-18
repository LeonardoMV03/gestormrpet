package net.martinezoft.ui.pages;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.domain.Dinero;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.components.CobroForm;
import net.martinezoft.ui.components.CobrosDetalleTable;
import net.martinezoft.ui.components.DeudoresTable;
import net.martinezoft.ui.viewmodels.CobrosViewModel;

/**
 * Pantalla de cobros pendientes: total adeudado por cliente y registro de pagos.
 */
public class CobrosPage extends VBox {

    @FXML
    private Label total;
    @FXML
    private StackPane slotDeudores;
    @FXML
    private StackPane slotVisitas;
    @FXML
    private StackPane slotFormulario;

    private final CobrosViewModel vm;

    public CobrosPage(CobrosViewModel vm) {
        this.vm = Objects.requireNonNull(vm, "El ViewModel de cobros es obligatorio");
        CargadorFxml.cargar(this, CobrosPage.class);

        total.textProperty().bind(Bindings.createStringBinding(
                () -> "Total adeudado: " + Dinero.formatearCentavos(vm.totalGeneralProperty().get()),
                vm.totalGeneralProperty()));
        slotDeudores.getChildren().setAll(new DeudoresTable(vm));
        slotVisitas.getChildren().setAll(new CobrosDetalleTable(vm));
        slotFormulario.getChildren().setAll(new CobroForm(vm));
        vm.cargar();
    }

    public void recargar() {
        vm.cargar();
    }
}
