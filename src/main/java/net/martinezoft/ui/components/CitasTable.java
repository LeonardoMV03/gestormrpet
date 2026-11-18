package net.martinezoft.ui.components;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.Dinero;
import net.martinezoft.ui.CargadorFxml;

/**
 * Tabla de visitas de la agenda. Solo presenta datos del ViewModel.
 */
public class CitasTable extends TableView<CitaDetalle> {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CitasTable(ObservableList<CitaDetalle> items, ObjectProperty<CitaDetalle> seleccionada,
                      String textoVacio) {
        Objects.requireNonNull(items, "Las visitas son obligatorias");
        Objects.requireNonNull(seleccionada, "La selección es obligatoria");
        CargadorFxml.cargar(this, CitasTable.class);

        TableColumn<CitaDetalle, String> columnaFecha = new TableColumn<>("Fecha y hora");
        columnaFecha.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().cita().fechaHora().format(FECHA_HORA)));
        columnaFecha.setPrefWidth(130);

        TableColumn<CitaDetalle, String> columnaMascota = new TableColumn<>("Mascota");
        columnaMascota.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().mascotaNombre()));
        columnaMascota.setPrefWidth(120);

        TableColumn<CitaDetalle, String> columnaCliente = new TableColumn<>("Cliente");
        columnaCliente.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(celda.getValue().clienteNombre()));
        columnaCliente.setPrefWidth(140);

        TableColumn<CitaDetalle, String> columnaServicios = new TableColumn<>("Servicios");
        columnaServicios.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().servicios().stream()
                        .map(servicio -> servicio.servicioNombre())
                        .collect(Collectors.joining(", "))));
        columnaServicios.setPrefWidth(180);

        TableColumn<CitaDetalle, String> columnaTotal = new TableColumn<>("Total");
        columnaTotal.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                Dinero.formatearCentavos(celda.getValue().totalCentavos())));
        columnaTotal.setPrefWidth(90);

        TableColumn<CitaDetalle, String> columnaEstado = new TableColumn<>("Estado");
        columnaEstado.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                celda.getValue().cita().estado().etiqueta()));
        columnaEstado.setPrefWidth(90);

        TableColumn<CitaDetalle, String> columnaPago = new TableColumn<>("Pago");
        columnaPago.setCellValueFactory(celda -> new ReadOnlyStringWrapper(
                pago(celda.getValue())));
        columnaPago.setPrefWidth(110);

        getColumns().setAll(columnaFecha, columnaMascota, columnaCliente, columnaServicios,
                columnaTotal, columnaEstado, columnaPago);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label(textoVacio));
        setItems(items);
        getSelectionModel().selectedItemProperty().addListener((observable, anterior, actual) -> {
            if (!Objects.equals(seleccionada.get(), actual)) {
                seleccionada.set(actual);
            }
        });
        seleccionada.addListener((observable, anterior, actual) -> {
            if (!Objects.equals(getSelectionModel().getSelectedItem(), actual)) {
                getSelectionModel().select(actual);
            }
        });
    }

    private static String pago(CitaDetalle detalle) {
        if (detalle.cita().estaPagada()) {
            return "Pagado · " + detalle.cita().metodoPago().etiqueta();
        }
        return detalle.cita().estadoPago().etiqueta();
    }
}
