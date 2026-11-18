package net.martinezoft.ui.viewmodels;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.Deudor;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.service.CitaService;

/**
 * Estado y comandos de cobros pendientes: deudores agrupados por cliente,
 * visitas sin pagar y registro del pago.
 */
public class CobrosViewModel {

    static final String MENSAJE_COBRADA = "Pago registrado";
    static final String MENSAJE_ERROR_CARGA = "No se pudieron cargar los cobros pendientes";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo completar la operación, intenta de nuevo";
    static final String MENSAJE_SIN_SELECCION = "Selecciona una visita para cobrar";
    static final String MENSAJE_METODO = "Selecciona un método de pago";

    private final CitaService citaService;

    private final ObservableList<Deudor> deudores = FXCollections.observableArrayList();
    private final ObservableList<CitaDetalle> pendientes = FXCollections.observableArrayList();
    private final ObservableList<CitaDetalle> visitas = FXCollections.observableArrayList();

    private final ObjectProperty<Deudor> deudorSeleccionado = new SimpleObjectProperty<>();
    private final ObjectProperty<CitaDetalle> visitaSeleccionada = new SimpleObjectProperty<>();
    private final ObjectProperty<MetodoPago> metodoPago = new SimpleObjectProperty<>(MetodoPago.EFECTIVO);

    private final StringProperty errorGeneral = new SimpleStringProperty("");
    private final StringProperty mensaje = new SimpleStringProperty("");
    private final LongProperty totalGeneral = new SimpleLongProperty(0);

    private final BooleanBinding puedeCobrar;

    public CobrosViewModel(CitaService citaService) {
        this.citaService = Objects.requireNonNull(citaService, "El servicio de visitas es obligatorio");
        this.puedeCobrar = Bindings.createBooleanBinding(
                () -> visitaSeleccionada.get() != null, visitaSeleccionada);
        deudorSeleccionado.addListener((observable, anterior, actual) -> {
            filtrar();
            errorGeneral.set("");
        });
        visitaSeleccionada.addListener((observable, anterior, actual) -> errorGeneral.set(""));
    }

    public void cargar() {
        limpiarErrores();
        mensaje.set("");
        Long deudorId = deudorSeleccionado.get() == null ? null : deudorSeleccionado.get().clienteId();
        Long visitaId = visitaSeleccionada.get() == null ? null : visitaSeleccionada.get().cita().id();
        try {
            pendientes.setAll(citaService.listarCobrosPendientes());
            deudores.setAll(citaService.listarDeudores());
            totalGeneral.set(pendientes.stream().mapToLong(CitaDetalle::totalCentavos).sum());
            Deudor deudor = deudorId == null ? null : deudores.stream()
                    .filter(actual -> Objects.equals(actual.clienteId(), deudorId))
                    .findFirst()
                    .orElse(null);
            deudorSeleccionado.set(deudor);
            filtrar();
            CitaDetalle visita = visitaId == null ? null : visitas.stream()
                    .filter(actual -> Objects.equals(actual.cita().id(), visitaId))
                    .findFirst()
                    .orElse(null);
            visitaSeleccionada.set(visita);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_CARGA);
        }
    }

    public void seleccionarDeudor(Deudor deudor) {
        deudorSeleccionado.set(deudor);
    }

    public void seleccionarVisita(CitaDetalle visita) {
        visitaSeleccionada.set(visita);
    }

    public void registrarPagoSeleccionado() {
        CitaDetalle actual = visitaSeleccionada.get();
        if (actual == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        if (metodoPago.get() == null) {
            errorGeneral.set(MENSAJE_METODO);
            return;
        }
        limpiarErrores();
        mensaje.set("");
        try {
            citaService.registrarPago(actual.cita().id(), metodoPago.get());
            cargar();
            mensaje.set(MENSAJE_COBRADA);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    private void filtrar() {
        Deudor deudor = deudorSeleccionado.get();
        if (deudor == null) {
            visitas.setAll(pendientes);
            return;
        }
        List<CitaDetalle> delCliente = pendientes.stream()
                .filter(cita -> Objects.equals(cita.clienteId(), deudor.clienteId()))
                .toList();
        visitas.setAll(delCliente);
    }

    private void limpiarErrores() {
        errorGeneral.set("");
    }

    public ObservableList<Deudor> deudores() {
        return deudores;
    }

    public ObservableList<CitaDetalle> visitas() {
        return visitas;
    }

    public ObjectProperty<Deudor> deudorSeleccionadoProperty() {
        return deudorSeleccionado;
    }

    public ObjectProperty<CitaDetalle> visitaSeleccionadaProperty() {
        return visitaSeleccionada;
    }

    public ObjectProperty<MetodoPago> metodoPagoProperty() {
        return metodoPago;
    }

    public StringProperty errorGeneralProperty() {
        return errorGeneral;
    }

    public StringProperty mensajeProperty() {
        return mensaje;
    }

    public LongProperty totalGeneralProperty() {
        return totalGeneral;
    }

    public BooleanBinding puedeCobrarProperty() {
        return puedeCobrar;
    }
}
