package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.Dinero;
import net.martinezoft.domain.Deudor;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.CobrosViewModel;

/**
 * Tabla de clientes con visitas atendidas sin pagar.
 */
public class DeudoresTable extends TableView<Deudor> {

    public DeudoresTable(CobrosViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de cobros es obligatorio");
        CargadorFxml.cargar(this, DeudoresTable.class);

        TableColumn<Deudor, String> columnaCliente = new TableColumn<>("Cliente");
        columnaCliente.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().clienteNombre()));
        columnaCliente.setPrefWidth(180);

        TableColumn<Deudor, String> columnaVisitas = new TableColumn<>("Visitas");
        columnaVisitas.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(String.valueOf(celda.getValue().visitas())));
        columnaVisitas.setPrefWidth(80);

        TableColumn<Deudor, String> columnaTotal = new TableColumn<>("Adeudado");
        columnaTotal.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                Dinero.formatearCentavos(celda.getValue().totalCentavos())));
        columnaTotal.setPrefWidth(110);

        getColumns().setAll(columnaCliente, columnaVisitas, columnaTotal);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("No hay cobros pendientes"));
        setItems(vm.deudores());
        getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(vm.deudorSeleccionadoProperty().get(), actual)) {
                vm.deudorSeleccionadoProperty().set(actual);
            }
        });
        vm.deudorSeleccionadoProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(getSelectionModel().getSelectedItem(), actual)) {
                getSelectionModel().select(actual);
            }
        });
    }
}
