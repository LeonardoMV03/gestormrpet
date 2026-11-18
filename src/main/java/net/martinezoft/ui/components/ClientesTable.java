package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.Cliente;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.ClientesViewModel;

/**
 * Tabla de clientes activos. Solo presenta los datos del ViewModel.
 */
public class ClientesTable extends TableView<Cliente> {

    public ClientesTable(ClientesViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de clientes es obligatorio");
        CargadorFxml.cargar(this, ClientesTable.class);

        TableColumn<Cliente, String> columnaNombre = new TableColumn<>("Nombre");
        columnaNombre.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().nombre()));
        columnaNombre.setPrefWidth(180);

        TableColumn<Cliente, String> columnaTelefono = new TableColumn<>("Teléfono");
        columnaTelefono.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().telefono() == null ? "" : celda.getValue().telefono()));
        columnaTelefono.setPrefWidth(130);

        TableColumn<Cliente, String> columnaEmail = new TableColumn<>("Email");
        columnaEmail.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().email() == null ? "" : celda.getValue().email()));
        columnaEmail.setPrefWidth(200);

        getColumns().setAll(columnaNombre, columnaTelefono, columnaEmail);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("No hay clientes"));
        setItems(vm.clientes());
        getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(vm.seleccionadoProperty().get(), actual)) {
                vm.seleccionadoProperty().set(actual);
            }
        });
        vm.seleccionadoProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(getSelectionModel().getSelectedItem(), actual)) {
                getSelectionModel().select(actual);
            }
        });
    }
}
