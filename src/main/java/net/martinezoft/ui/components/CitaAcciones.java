package net.martinezoft.ui.components;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.Dinero;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.AgendaViewModel;

/**
 * Detalle de la visita seleccionada en la agenda, con acciones de estado y
 * cobro. La confirmación de cancelación la maneja la página.
 */
public class CitaAcciones extends VBox {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label titulo;
    @FXML
    private Label detalle;
    @FXML
    private Label servicios;
    @FXML
    private Label total;
    @FXML
    private Label pago;
    @FXML
    private Button botonAtender;
    @FXML
    private Button botonCancelar;
    @FXML
    private ComboBox<MetodoPago> metodo;
    @FXML
    private Button botonCobrar;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    public CitaAcciones(AgendaViewModel vm, Runnable alCancelar) {
        Objects.requireNonNull(vm, "El ViewModel de agenda es obligatorio");
        Objects.requireNonNull(alCancelar, "La acción de cancelar es obligatoria");
        CargadorFxml.cargar(this, CitaAcciones.class);

        metodo.setItems(FXCollections.observableArrayList(MetodoPago.values()));
        metodo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MetodoPago valor) {
                return valor == null ? "" : valor.etiqueta();
            }

            @Override
            public MetodoPago fromString(String texto) {
                return null;
            }
        });
        metodo.valueProperty().bindBidirectional(vm.metodoPagoProperty());

        titulo.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.seleccionadaProperty().get();
            return seleccionada == null ? "Selecciona una visita" : "Visita de " + seleccionada.mascotaNombre();
        }, vm.seleccionadaProperty()));

        detalle.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.seleccionadaProperty().get();
            if (seleccionada == null) {
                return "Elige una visita en Hoy o Próximas para ver su detalle.";
            }
            return seleccionada.cita().fechaHora().format(FECHA_HORA)
                    + " · " + seleccionada.clienteNombre()
                    + " · " + seleccionada.cita().estado().etiqueta();
        }, vm.seleccionadaProperty()));

        servicios.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.seleccionadaProperty().get();
            if (seleccionada == null) {
                return "";
            }
            return seleccionada.servicios().stream()
                    .map(servicio -> servicio.servicioNombre() + " · "
                            + Dinero.formatearCentavos(servicio.precioCentavos()))
                    .collect(Collectors.joining("\n"));
        }, vm.seleccionadaProperty()));

        total.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.seleccionadaProperty().get();
            return textoTotal(seleccionada);
        }, vm.seleccionadaProperty()));

        pago.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.seleccionadaProperty().get();
            if (seleccionada == null) {
                return "";
            }
            if (seleccionada.cita().estaPagada()) {
                return "Pagado con " + seleccionada.cita().metodoPago().etiqueta().toLowerCase()
                        + " el " + seleccionada.cita().pagadoEn()
                                .atZone(ZoneId.systemDefault()).format(FECHA_HORA);
            }
            if (seleccionada.cita().estaAtendida()) {
                return "A deber";
            }
            return "Sin cobro";
        }, vm.seleccionadaProperty()));

        botonAtender.disableProperty().bind(vm.puedeAtenderProperty().not());
        botonCancelar.disableProperty().bind(vm.puedeCancelarProperty().not());
        botonCobrar.disableProperty().bind(vm.puedeCobrarProperty().not());
        metodo.disableProperty().bind(vm.puedeCobrarProperty().not());

        botonAtender.setOnAction(e -> vm.atenderSeleccionada());
        botonCancelar.setOnAction(e -> alCancelar.run());
        botonCobrar.setOnAction(e -> vm.registrarPagoSeleccionado());

        etiquetaMensaje.textProperty().bind(vm.mensajeProperty());
        etiquetaMensaje.visibleProperty().bind(vm.mensajeProperty().isNotEmpty());
        etiquetaMensaje.managedProperty().bind(etiquetaMensaje.visibleProperty());

        etiquetaError.textProperty().bind(vm.errorGeneralProperty());
        etiquetaError.visibleProperty().bind(vm.errorGeneralProperty().isNotEmpty());
        etiquetaError.managedProperty().bind(etiquetaError.visibleProperty());
    }

    private static String textoTotal(CitaDetalle seleccionada) {
        return seleccionada == null ? ""
                : "Total: " + Dinero.formatearCentavos(seleccionada.totalCentavos());
    }
}
