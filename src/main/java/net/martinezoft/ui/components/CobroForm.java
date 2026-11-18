package net.martinezoft.ui.components;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

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
import net.martinezoft.ui.viewmodels.CobrosViewModel;

/**
 * Panel para registrar el pago de la visita seleccionada en cobros pendientes.
 */
public class CobroForm extends VBox {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label titulo;
    @FXML
    private Label detalle;
    @FXML
    private Label total;
    @FXML
    private ComboBox<MetodoPago> metodo;
    @FXML
    private Button botonCobrar;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    public CobroForm(CobrosViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de cobros es obligatorio");
        CargadorFxml.cargar(this, CobroForm.class);

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
            CitaDetalle seleccionada = vm.visitaSeleccionadaProperty().get();
            return seleccionada == null ? "Sin visita seleccionada" : "Visita de " + seleccionada.mascotaNombre();
        }, vm.visitaSeleccionadaProperty()));

        detalle.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.visitaSeleccionadaProperty().get();
            if (seleccionada == null) {
                return "Elige una visita de la lista para registrar el pago.";
            }
            return seleccionada.cita().fechaHora().format(FECHA_HORA)
                    + " · " + seleccionada.clienteNombre();
        }, vm.visitaSeleccionadaProperty()));

        total.textProperty().bind(Bindings.createStringBinding(() -> {
            CitaDetalle seleccionada = vm.visitaSeleccionadaProperty().get();
            return seleccionada == null ? ""
                    : "Total: " + Dinero.formatearCentavos(seleccionada.totalCentavos());
        }, vm.visitaSeleccionadaProperty()));

        botonCobrar.disableProperty().bind(vm.puedeCobrarProperty().not());
        metodo.disableProperty().bind(vm.puedeCobrarProperty().not());
        botonCobrar.setOnAction(e -> vm.registrarPagoSeleccionado());

        etiquetaMensaje.textProperty().bind(vm.mensajeProperty());
        etiquetaMensaje.visibleProperty().bind(vm.mensajeProperty().isNotEmpty());
        etiquetaMensaje.managedProperty().bind(etiquetaMensaje.visibleProperty());

        etiquetaError.textProperty().bind(vm.errorGeneralProperty());
        etiquetaError.visibleProperty().bind(vm.errorGeneralProperty().isNotEmpty());
        etiquetaError.managedProperty().bind(etiquetaError.visibleProperty());
    }
}
