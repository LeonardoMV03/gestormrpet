package net.martinezoft.ui.viewmodels;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.MascotaConDueno;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.domain.Servicio;
import net.martinezoft.service.CitaService;

/**
 * Estado y comandos de la agenda: visitas de hoy y próximas, alta de visitas,
 * cambios de estado y cobro. No conoce vistas ni el toolkit.
 */
public class AgendaViewModel {

    static final String MENSAJE_AGENDADA = "Visita agendada";
    static final String MENSAJE_ATENDIDA = "Visita marcada como atendida";
    static final String MENSAJE_CANCELADA = "Visita cancelada";
    static final String MENSAJE_COBRADA = "Pago registrado";
    static final String MENSAJE_ERROR_CARGA = "No se pudieron cargar las visitas";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo completar la operación, intenta de nuevo";
    static final String MENSAJE_SIN_SELECCION = "Selecciona una visita";
    static final String MENSAJE_METODO = "Selecciona un método de pago";

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");

    private final CitaService citaService;
    private final Clock reloj;

    private final ObservableList<MascotaConDueno> mascotas = FXCollections.observableArrayList();
    private final ObservableList<Servicio> servicios = FXCollections.observableArrayList();
    private final ObservableList<Servicio> serviciosSeleccionados = FXCollections.observableArrayList();
    private final ObservableList<CitaDetalle> citasHoy = FXCollections.observableArrayList();
    private final ObservableList<CitaDetalle> citasProximas = FXCollections.observableArrayList();

    private final ObjectProperty<CitaDetalle> seleccionada = new SimpleObjectProperty<>();
    private final ObjectProperty<MascotaConDueno> mascotaSeleccionada = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> fechaCita = new SimpleObjectProperty<>();
    private final StringProperty horaTexto = new SimpleStringProperty("");
    private final StringProperty notas = new SimpleStringProperty("");
    private final ObjectProperty<MetodoPago> metodoPago = new SimpleObjectProperty<>(MetodoPago.EFECTIVO);

    private final StringProperty errorMascota = new SimpleStringProperty("");
    private final StringProperty errorServicios = new SimpleStringProperty("");
    private final StringProperty errorFechaHora = new SimpleStringProperty("");
    private final StringProperty errorGeneral = new SimpleStringProperty("");
    private final StringProperty mensaje = new SimpleStringProperty("");
    private final LongProperty totalSeleccionado = new SimpleLongProperty(0);

    private final BooleanBinding puedeAgendar;
    private final BooleanBinding puedeAtender;
    private final BooleanBinding puedeCancelar;
    private final BooleanBinding puedeCobrar;

    public AgendaViewModel(CitaService citaService) {
        this(citaService, Clock.systemDefaultZone());
    }

    public AgendaViewModel(CitaService citaService, Clock reloj) {
        this.citaService = Objects.requireNonNull(citaService, "El servicio de visitas es obligatorio");
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio");
        fechaCita.set(LocalDate.now(reloj));

        this.puedeAgendar = Bindings.createBooleanBinding(
                () -> mascotaSeleccionada.get() != null
                        && fechaCita.get() != null
                        && horaValida(horaTexto.get())
                        && !serviciosSeleccionados.isEmpty(),
                mascotaSeleccionada, fechaCita, horaTexto, serviciosSeleccionados);
        this.puedeAtender = Bindings.createBooleanBinding(
                () -> seleccionada.get() != null && seleccionada.get().cita().estaPendiente(),
                seleccionada);
        this.puedeCancelar = Bindings.createBooleanBinding(
                () -> seleccionada.get() != null && seleccionada.get().cita().estaPendiente(),
                seleccionada);
        this.puedeCobrar = Bindings.createBooleanBinding(
                () -> seleccionada.get() != null
                        && seleccionada.get().cita().estaAtendida()
                        && !seleccionada.get().cita().estaPagada(),
                seleccionada);

        serviciosSeleccionados.addListener((ListChangeListener<Servicio>) cambio ->
                totalSeleccionado.set(calcularTotal(serviciosSeleccionados)));
        mascotaSeleccionada.addListener((observable, anterior, actual) -> {
            if (actual != null) {
                errorMascota.set("");
            }
        });
        serviciosSeleccionados.addListener((ListChangeListener<Servicio>) cambio -> {
            if (!serviciosSeleccionados.isEmpty()) {
                errorServicios.set("");
            }
        });
        horaTexto.addListener((observable, anterior, actual) -> validarHoraEnVivo(actual));
        seleccionada.addListener((observable, anterior, actual) -> {
            mensaje.set("");
            errorGeneral.set("");
        });
    }

    public void cargar() {
        limpiarErrores();
        mensaje.set("");
        Long idSeleccionado = seleccionada.get() == null ? null : seleccionada.get().cita().id();
        try {
            mascotas.setAll(citaService.listarMascotasDisponibles());
            servicios.setAll(citaService.listarServiciosDisponibles());
            serviciosSeleccionados.removeIf(servicio ->
                    servicios.stream().noneMatch(actual -> Objects.equals(actual.id(), servicio.id())));
            recargarSeleccionando(idSeleccionado);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_CARGA);
        }
    }

    public void nuevo() {
        limpiarErrores();
        mensaje.set("");
        seleccionada.set(null);
        mascotaSeleccionada.set(null);
        serviciosSeleccionados.clear();
        horaTexto.set("");
        notas.set("");
        fechaCita.set(LocalDate.now(reloj));
        metodoPago.set(MetodoPago.EFECTIVO);
    }

    public void seleccionar(CitaDetalle cita) {
        seleccionada.set(cita);
        if (cita != null) {
            metodoPago.set(MetodoPago.EFECTIVO);
        }
    }

    public void agendar() {
        limpiarErrores();
        mensaje.set("");
        try {
            LocalDateTime fechaHora = fechaHora();
            List<Long> servicioIds = serviciosSeleccionados.stream().map(Servicio::id).toList();
            MascotaConDueno mascota = mascotaSeleccionada.get();
            CitaDetalle creada = citaService.agendar(
                    mascota == null ? null : mascota.mascota().id(),
                    servicioIds, fechaHora, notas.get());
            serviciosSeleccionados.clear();
            horaTexto.set("");
            notas.set("");
            recargarSeleccionando(creada.cita().id());
            mensaje.set(MENSAJE_AGENDADA);
        } catch (IllegalArgumentException e) {
            aplicarError(e);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    public void atenderSeleccionada() {
        CitaDetalle actual = seleccionada.get();
        if (actual == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        limpiarErrores();
        mensaje.set("");
        try {
            CitaDetalle actualizada = citaService.atender(actual.cita().id());
            recargarSeleccionando(actualizada.cita().id());
            mensaje.set(MENSAJE_ATENDIDA);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    public void cancelarSeleccionada() {
        CitaDetalle actual = seleccionada.get();
        if (actual == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        limpiarErrores();
        mensaje.set("");
        try {
            CitaDetalle actualizada = citaService.cancelar(actual.cita().id());
            recargarSeleccionando(actualizada.cita().id());
            mensaje.set(MENSAJE_CANCELADA);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    public void registrarPagoSeleccionado() {
        CitaDetalle actual = seleccionada.get();
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
            CitaDetalle actualizada = citaService.registrarPago(actual.cita().id(), metodoPago.get());
            recargarSeleccionando(actualizada.cita().id());
            mensaje.set(MENSAJE_COBRADA);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    private void recargarSeleccionando(Long idSeleccionado) throws SQLException {
        LocalDate hoy = LocalDate.now(reloj);
        List<CitaDetalle> delDia = citaService.listarHoy(hoy);
        List<CitaDetalle> proximas = citaService.listarProximas(hoy);
        citasHoy.setAll(delDia);
        citasProximas.setAll(proximas);
        CitaDetalle encontrada = null;
        if (idSeleccionado != null) {
            encontrada = delDia.stream()
                    .filter(cita -> Objects.equals(cita.cita().id(), idSeleccionado))
                    .findFirst()
                    .orElseGet(() -> proximas.stream()
                            .filter(cita -> Objects.equals(cita.cita().id(), idSeleccionado))
                            .findFirst()
                            .orElse(null));
        }
        seleccionada.set(encontrada);
    }

    private LocalDateTime fechaHora() {
        LocalDate fecha = fechaCita.get();
        if (fecha == null) {
            throw new IllegalArgumentException("Selecciona la fecha de la visita");
        }
        LocalTime hora = parsearHora(horaTexto.get());
        return LocalDateTime.of(fecha, hora);
    }

    private void validarHoraEnVivo(String texto) {
        if (texto == null || texto.isBlank()) {
            errorFechaHora.set("");
            return;
        }
        try {
            parsearHora(texto);
            errorFechaHora.set("");
        } catch (IllegalArgumentException e) {
            errorFechaHora.set(e.getMessage());
        }
    }

    private void aplicarError(IllegalArgumentException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if (CitaService.MENSAJE_MASCOTA_INVALIDA.equals(texto)) {
            errorMascota.set(texto);
        } else if (texto.startsWith("La fecha") || texto.startsWith("La hora")
                || texto.startsWith("Selecciona la fecha")
                || CitaService.MENSAJE_HORARIO_OCUPADO.equals(texto)) {
            errorFechaHora.set(texto);
        } else if (CitaService.MENSAJE_SIN_SERVICIOS.equals(texto)
                || CitaService.MENSAJE_SERVICIO_DUPLICADO.equals(texto)
                || CitaService.MENSAJE_SERVICIO_INVALIDO.equals(texto)) {
            errorServicios.set(texto);
        } else {
            errorGeneral.set(texto);
        }
    }

    private void limpiarErrores() {
        errorMascota.set("");
        errorServicios.set("");
        errorFechaHora.set("");
        errorGeneral.set("");
    }

    private static boolean horaValida(String texto) {
        try {
            parsearHora(texto);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static LocalTime parsearHora(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La hora debe tener formato HH:mm");
        }
        try {
            return LocalTime.parse(texto.trim(), FORMATO_HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe tener formato HH:mm", e);
        }
    }

    private static long calcularTotal(List<Servicio> servicios) {
        return servicios.stream().mapToLong(Servicio::precioCentavos).sum();
    }

    public ObservableList<MascotaConDueno> mascotas() {
        return mascotas;
    }

    public ObservableList<Servicio> servicios() {
        return servicios;
    }

    public ObservableList<Servicio> serviciosSeleccionados() {
        return serviciosSeleccionados;
    }

    public ObservableList<CitaDetalle> citasHoy() {
        return citasHoy;
    }

    public ObservableList<CitaDetalle> citasProximas() {
        return citasProximas;
    }

    public ObjectProperty<CitaDetalle> seleccionadaProperty() {
        return seleccionada;
    }

    public ObjectProperty<MascotaConDueno> mascotaSeleccionadaProperty() {
        return mascotaSeleccionada;
    }

    public ObjectProperty<LocalDate> fechaCitaProperty() {
        return fechaCita;
    }

    public StringProperty horaTextoProperty() {
        return horaTexto;
    }

    public StringProperty notasProperty() {
        return notas;
    }

    public ObjectProperty<MetodoPago> metodoPagoProperty() {
        return metodoPago;
    }

    public StringProperty errorMascotaProperty() {
        return errorMascota;
    }

    public StringProperty errorServiciosProperty() {
        return errorServicios;
    }

    public StringProperty errorFechaHoraProperty() {
        return errorFechaHora;
    }

    public StringProperty errorGeneralProperty() {
        return errorGeneral;
    }

    public StringProperty mensajeProperty() {
        return mensaje;
    }

    public LongProperty totalSeleccionadoProperty() {
        return totalSeleccionado;
    }

    public BooleanBinding puedeAgendarProperty() {
        return puedeAgendar;
    }

    public BooleanBinding puedeAtenderProperty() {
        return puedeAtender;
    }

    public BooleanBinding puedeCancelarProperty() {
        return puedeCancelar;
    }

    public BooleanBinding puedeCobrarProperty() {
        return puedeCobrar;
    }
}
