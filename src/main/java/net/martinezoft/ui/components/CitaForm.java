package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import net.martinezoft.domain.Dinero;
import net.martinezoft.domain.MascotaConDueno;
import net.martinezoft.domain.Servicio;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.AgendaViewModel;

/**
 * Formulario para agendar una visita: mascota, servicios, fecha, hora y notas.
 */
public class CitaForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;
    @FXML
    private Label etiquetaAyuda;
    @FXML
    private Label etiquetaMensaje;
    @FXML
    private Label etiquetaError;

    public CitaForm(AgendaViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de agenda es obligatorio");
        CargadorFxml.cargar(this, CitaForm.class);

        ComboBox<MascotaConDueno> campoMascota = new ComboBox<>();
        campoMascota.setItems(vm.mascotas());
        campoMascota.setMaxWidth(Double.MAX_VALUE);
        campoMascota.setConverter(new StringConverter<>() {
            @Override
            public String toString(MascotaConDueno mascota) {
                return mascota == null ? "" : mascota.etiqueta();
            }

            @Override
            public MascotaConDueno fromString(String texto) {
                return null;
            }
        });
        campoMascota.valueProperty().bindBidirectional(vm.mascotaSeleccionadaProperty());

        ListView<Servicio> campoServicios = new ListView<>(vm.servicios());
        campoServicios.setPrefHeight(150);
        campoServicios.getStyleClass().add("lista-servicios");
        campoServicios.setCellFactory(lista -> new ServicioCheckCell(vm));
        vm.serviciosSeleccionados().addListener(
                (ListChangeListener<Servicio>) cambio -> campoServicios.refresh());

        DatePicker campoFecha = new DatePicker();
        campoFecha.setMaxWidth(Double.MAX_VALUE);
        campoFecha.valueProperty().bindBidirectional(vm.fechaCitaProperty());

        TextField campoHora = new TextField();
        campoHora.setPromptText("09:30");
        campoHora.textProperty().bindBidirectional(vm.horaTextoProperty());

        TextField campoNotas = new TextField();
        campoNotas.setPromptText("Notas de la visita");
        campoNotas.textProperty().bindBidirectional(vm.notasProperty());

        FormField formMascota = new FormField("Mascota", campoMascota);
        formMascota.errorProperty().bind(vm.errorMascotaProperty());
        FormField formServicios = new FormField("Servicios", campoServicios);
        formServicios.errorProperty().bind(vm.errorServiciosProperty());
        FormField formFecha = new FormField("Fecha", campoFecha);
        FormField formHora = new FormField("Hora", campoHora);
        formHora.errorProperty().bind(vm.errorFechaHoraProperty());
        FormField formNotas = new FormField("Notas", campoNotas);

        Label total = new Label();
        total.getStyleClass().add("texto-secundario");
        total.textProperty().bind(Bindings.createStringBinding(
                () -> "Total: " + Dinero.formatearCentavos(vm.totalSeleccionadoProperty().get()),
                vm.totalSeleccionadoProperty()));

        campos.getChildren().setAll(formMascota, formServicios, formFecha, formHora, formNotas, total);

        Button botonNuevo = new Button("Nueva");
        Button botonAgendar = new Button("Agendar");
        botonAgendar.getStyleClass().add("boton-primario");
        botonAgendar.disableProperty().bind(vm.puedeAgendarProperty().not());
        botonNuevo.setOnAction(e -> vm.nuevo());
        botonAgendar.setOnAction(e -> vm.agendar());
        zonaAccion.getChildren().setAll(botonNuevo, botonAgendar);

        etiquetaAyuda.setText("Elige mascota, al menos un servicio y una hora válida");
        etiquetaAyuda.visibleProperty().bind(vm.puedeAgendarProperty().not());
        etiquetaAyuda.managedProperty().bind(etiquetaAyuda.visibleProperty());

        etiquetaMensaje.textProperty().bind(vm.mensajeProperty());
        etiquetaMensaje.visibleProperty().bind(vm.mensajeProperty().isNotEmpty());
        etiquetaMensaje.managedProperty().bind(etiquetaMensaje.visibleProperty());

        etiquetaError.textProperty().bind(vm.errorGeneralProperty());
        etiquetaError.visibleProperty().bind(vm.errorGeneralProperty().isNotEmpty());
        etiquetaError.managedProperty().bind(etiquetaError.visibleProperty());
    }

    private static final class ServicioCheckCell extends ListCell<Servicio> {

        private final AgendaViewModel vm;
        private final CheckBox check = new CheckBox();

        ServicioCheckCell(AgendaViewModel vm) {
            this.vm = vm;
            check.setOnAction(e -> alternar());
        }

        @Override
        protected void updateItem(Servicio servicio, boolean vacia) {
            super.updateItem(servicio, vacia);
            if (vacia || servicio == null) {
                setGraphic(null);
                return;
            }
            check.setText(servicio.nombre() + " · " + Dinero.formatearCentavos(servicio.precioCentavos()));
            check.setSelected(estaSeleccionado(servicio));
            setGraphic(check);
        }

        private void alternar() {
            Servicio servicio = getItem();
            if (servicio == null) {
                return;
            }
            if (check.isSelected()) {
                if (!estaSeleccionado(servicio)) {
                    vm.serviciosSeleccionados().add(servicio);
                }
            } else {
                vm.serviciosSeleccionados().removeIf(actual -> coincide(actual, servicio));
            }
        }

        private boolean estaSeleccionado(Servicio servicio) {
            return vm.serviciosSeleccionados().stream()
                    .anyMatch(actual -> coincide(actual, servicio));
        }

        private static boolean coincide(Servicio uno, Servicio otro) {
            return Objects.equals(uno.id(), otro.id());
        }
    }
}
