package net.martinezoft.ui.components;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.Dinero;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.CobrosViewModel;

/**
 * Tabla de visitas sin pagar del deudor seleccionado.
 */
public class CobrosDetalleTable extends TableView<CitaDetalle> {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CobrosDetalleTable(CobrosViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de cobros es obligatorio");
        CargadorFxml.cargar(this, CobrosDetalleTable.class);

        TableColumn<CitaDetalle, String> columnaFecha = new TableColumn<>("Fecha");
        columnaFecha.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().cita().fechaHora().format(FECHA_HORA)));
        columnaFecha.setPrefWidth(130);

        TableColumn<CitaDetalle, String> columnaMascota = new TableColumn<>("Mascota");
        columnaMascota.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().mascotaNombre()));
        columnaMascota.setPrefWidth(130);

        TableColumn<CitaDetalle, String> columnaCliente = new TableColumn<>("Cliente");
        columnaCliente.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().clienteNombre()));
        columnaCliente.setPrefWidth(150);

        TableColumn<CitaDetalle, String> columnaServicios = new TableColumn<>("Servicios");
        columnaServicios.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().servicios().stream()
                        .map(servicio -> servicio.servicioNombre())
                        .collect(Collectors.joining(", "))));
        columnaServicios.setPrefWidth(180);

        TableColumn<CitaDetalle, String> columnaTotal = new TableColumn<>("Total");
        columnaTotal.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                Dinero.formatearCentavos(celda.getValue().totalCentavos())));
        columnaTotal.setPrefWidth(100);

        getColumns().setAll(columnaFecha, columnaMascota, columnaCliente, columnaServicios, columnaTotal);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("Selecciona un deudor o no hay visitas pendientes"));
        setItems(vm.visitas());
        getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(vm.visitaSeleccionadaProperty().get(), actual)) {
                vm.visitaSeleccionadaProperty().set(actual);
            }
        });
        vm.visitaSeleccionadaProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(getSelectionModel().getSelectedItem(), actual)) {
                getSelectionModel().select(actual);
            }
        });
    }
}
