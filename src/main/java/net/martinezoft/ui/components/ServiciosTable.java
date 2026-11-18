package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.Dinero;
import net.martinezoft.domain.Servicio;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.ServiciosViewModel;

/**
 * Tabla de servicios activos. Solo presenta los datos del ViewModel; no aplica
 * reglas de negocio ni accede a la base de datos.
 */
public class ServiciosTable extends TableView<Servicio> {

    public ServiciosTable(ServiciosViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de servicios es obligatorio");
        CargadorFxml.cargar(this, ServiciosTable.class);

        TableColumn<Servicio, String> columnaNombre = new TableColumn<>("Nombre");
        columnaNombre.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().nombre()));
        columnaNombre.setPrefWidth(160);

        TableColumn<Servicio, String> columnaDescripcion = new TableColumn<>("Descripción");
        columnaDescripcion.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().descripcion() == null ? "" : celda.getValue().descripcion()));
        columnaDescripcion.setPrefWidth(240);

        TableColumn<Servicio, String> columnaPrecio = new TableColumn<>("Precio");
        columnaPrecio.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                Dinero.formatearCentavos(celda.getValue().precioCentavos())));
        columnaPrecio.setPrefWidth(110);

        TableColumn<Servicio, String> columnaDuracion = new TableColumn<>("Duración");
        columnaDuracion.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().duracionMin() == null
                        ? "—"
                        : celda.getValue().duracionMin() + " min"));
        columnaDuracion.setPrefWidth(100);

        getColumns().setAll(columnaNombre, columnaDescripcion, columnaPrecio, columnaDuracion);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("No hay servicios activos"));
        setItems(vm.servicios());
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
