package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.Mascota;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.MascotasViewModel;

/**
 * Tabla de mascotas activas del cliente seleccionado. Solo presenta los datos
 * del ViewModel.
 */
public class MascotasTable extends TableView<Mascota> {

    public MascotasTable(MascotasViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de mascotas es obligatorio");
        CargadorFxml.cargar(this, MascotasTable.class);

        TableColumn<Mascota, String> columnaNombre = new TableColumn<>("Nombre");
        columnaNombre.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().nombre()));
        columnaNombre.setPrefWidth(150);

        TableColumn<Mascota, String> columnaEspecie = new TableColumn<>("Especie");
        columnaEspecie.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().especie()));
        columnaEspecie.setPrefWidth(100);

        TableColumn<Mascota, String> columnaRaza = new TableColumn<>("Raza");
        columnaRaza.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().raza() == null ? "" : celda.getValue().raza()));
        columnaRaza.setPrefWidth(130);

        TableColumn<Mascota, String> columnaSexo = new TableColumn<>("Sexo");
        columnaSexo.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().sexo() == null ? "" : celda.getValue().sexo()));
        columnaSexo.setPrefWidth(90);

        getColumns().setAll(columnaNombre, columnaEspecie, columnaRaza, columnaSexo);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("Selecciona un cliente para ver sus mascotas"));
        setItems(vm.mascotas());
        getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(vm.seleccionadaProperty().get(), actual)) {
                vm.seleccionadaProperty().set(actual);
            }
        });
        vm.seleccionadaProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(getSelectionModel().getSelectedItem(), actual)) {
                getSelectionModel().select(actual);
            }
        });
    }
}
