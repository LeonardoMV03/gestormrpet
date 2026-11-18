package net.martinezoft.ui.viewmodels;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.martinezoft.domain.Dinero;
import net.martinezoft.domain.Servicio;
import net.martinezoft.service.ServicioService;

/**
 * Estado y comandos de la pantalla de servicios. No conoce vistas ni el
 * toolkit: solo propiedades, validaciones y llamadas al servicio.
 */
public class ServiciosViewModel {

    static final String MENSAJE_GUARDADO = "Servicio guardado";
    static final String MENSAJE_DESACTIVADO = "Servicio desactivado";
    static final String MENSAJE_ERROR_CARGA = "No se pudieron cargar los servicios";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo completar la operación, intenta de nuevo";
    static final String MENSAJE_SIN_SELECCION = "Selecciona un servicio para desactivarlo";

    private final ServicioService servicioService;
    private final ObservableList<Servicio> servicios = FXCollections.observableArrayList();
    private final ObjectProperty<Servicio> seleccionado = new SimpleObjectProperty<>();

    private final StringProperty nombre = new SimpleStringProperty("");
    private final StringProperty descripcion = new SimpleStringProperty("");
    private final StringProperty precioTexto = new SimpleStringProperty("");
    private final StringProperty duracionTexto = new SimpleStringProperty("");

    private final StringProperty errorNombre = new SimpleStringProperty("");
    private final StringProperty errorPrecio = new SimpleStringProperty("");
    private final StringProperty errorDuracion = new SimpleStringProperty("");
    private final StringProperty errorGeneral = new SimpleStringProperty("");
    private final StringProperty mensaje = new SimpleStringProperty("");
    private final BooleanProperty editando = new SimpleBooleanProperty(false);

    private final BooleanBinding puedeGuardar;
    private boolean sincronizando;

    public ServiciosViewModel(ServicioService servicioService) {
        this.servicioService = Objects.requireNonNull(servicioService,
                "El servicio de servicios es obligatorio");
        this.puedeGuardar = Bindings.createBooleanBinding(
                () -> !textoVacio(nombre.get()) && precioValido(precioTexto.get())
                        && duracionValida(duracionTexto.get()),
                nombre, precioTexto, duracionTexto);
        seleccionado.addListener((observable, anterior, actual) -> {
            if (!sincronizando) {
                volcarFormulario(actual);
            }
        });
    }

    public void cargar() {
        limpiarErrores();
        mensaje.set("");
        Long idSeleccionado = seleccionado.get() == null ? null : seleccionado.get().id();
        try {
            recargarSeleccionando(idSeleccionado);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_CARGA);
        }
    }

    public void nuevo() {
        limpiarErrores();
        mensaje.set("");
        sincronizando = true;
        try {
            seleccionado.set(null);
        } finally {
            sincronizando = false;
        }
        limpiarCampos();
    }

    public void seleccionar(Servicio servicio) {
        seleccionado.set(servicio);
    }

    public void guardar() {
        limpiarErrores();
        mensaje.set("");
        if (!validarCampos()) {
            return;
        }
        try {
            long precio = Dinero.parsearCentavos(precioTexto.get());
            Integer duracion = parsearDuracion(duracionTexto.get());
            String nombreLimpio = nombre.get().trim();
            String descripcionLimpia = descripcion.get();

            Servicio actual = seleccionado.get();
            Servicio guardado;
            if (editando.get() && actual != null && actual.id() != null) {
                guardado = servicioService.actualizar(new Servicio(actual.id(), nombreLimpio,
                        descripcionLimpia, precio, duracion, true));
            } else {
                guardado = servicioService.crear(
                        Servicio.nuevo(nombreLimpio, descripcionLimpia, precio, duracion));
            }
            recargarSeleccionando(guardado.id());
            mensaje.set(MENSAJE_GUARDADO);
        } catch (IllegalArgumentException e) {
            aplicarError(e);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    public void desactivarSeleccionado() {
        limpiarErrores();
        mensaje.set("");
        Servicio actual = seleccionado.get();
        if (actual == null || actual.id() == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        try {
            servicioService.desactivar(actual.id());
            recargarSeleccionando(null);
            mensaje.set(MENSAJE_DESACTIVADO);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    private void recargarSeleccionando(Long id) throws SQLException {
        List<Servicio> activos = servicioService.listar();
        servicios.setAll(activos);
        if (id == null) {
            seleccionado.set(null);
            return;
        }
        Servicio encontrado = activos.stream()
                .filter(servicio -> id.equals(servicio.id()))
                .findFirst()
                .orElse(null);
        seleccionado.set(encontrado);
    }

    private void volcarFormulario(Servicio servicio) {
        if (sincronizando) {
            return;
        }
        sincronizando = true;
        try {
            limpiarErrores();
            mensaje.set("");
            if (servicio == null) {
                limpiarCampos();
            } else {
                nombre.set(servicio.nombre());
                descripcion.set(servicio.descripcion() == null ? "" : servicio.descripcion());
                precioTexto.set(Dinero.formatearCentavos(servicio.precioCentavos()));
                duracionTexto.set(servicio.duracionMin() == null ? ""
                        : String.valueOf(servicio.duracionMin()));
                editando.set(true);
            }
        } finally {
            sincronizando = false;
        }
    }

    private void limpiarCampos() {
        nombre.set("");
        descripcion.set("");
        precioTexto.set("");
        duracionTexto.set("");
        editando.set(false);
    }

    private boolean validarCampos() {
        boolean valido = true;
        if (textoVacio(nombre.get())) {
            errorNombre.set("El nombre del servicio es obligatorio");
            valido = false;
        }
        try {
            Dinero.parsearCentavos(precioTexto.get());
        } catch (IllegalArgumentException e) {
            errorPrecio.set(e.getMessage());
            valido = false;
        }
        try {
            parsearDuracion(duracionTexto.get());
        } catch (IllegalArgumentException e) {
            errorDuracion.set(e.getMessage());
            valido = false;
        }
        return valido;
    }

    private void aplicarError(IllegalArgumentException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if (texto.equals(ServicioService.MENSAJE_NOMBRE_DUPLICADO) || texto.startsWith("El nombre")) {
            errorNombre.set(texto);
        } else if (texto.startsWith("El precio")) {
            errorPrecio.set(texto);
        } else if (texto.startsWith("La duración")) {
            errorDuracion.set(texto);
        } else {
            errorGeneral.set(texto);
        }
    }

    private void limpiarErrores() {
        errorNombre.set("");
        errorPrecio.set("");
        errorDuracion.set("");
        errorGeneral.set("");
    }

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static boolean precioValido(String texto) {
        try {
            Dinero.parsearCentavos(texto);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean duracionValida(String texto) {
        try {
            parsearDuracion(texto);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Integer parsearDuracion(String texto) {
        if (textoVacio(texto)) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.startsWith("-")) {
            throw new IllegalArgumentException("La duración no puede ser negativa");
        }
        if (!limpio.matches("\\d+")) {
            throw new IllegalArgumentException("La duración debe ser un número entero de minutos");
        }
        try {
            return Integer.valueOf(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La duración es demasiado grande", e);
        }
    }

    public ObservableList<Servicio> servicios() {
        return servicios;
    }

    public ObjectProperty<Servicio> seleccionadoProperty() {
        return seleccionado;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public StringProperty precioTextoProperty() {
        return precioTexto;
    }

    public StringProperty duracionTextoProperty() {
        return duracionTexto;
    }

    public StringProperty errorNombreProperty() {
        return errorNombre;
    }

    public StringProperty errorPrecioProperty() {
        return errorPrecio;
    }

    public StringProperty errorDuracionProperty() {
        return errorDuracion;
    }

    public StringProperty errorGeneralProperty() {
        return errorGeneral;
    }

    public StringProperty mensajeProperty() {
        return mensaje;
    }

    public BooleanProperty editandoProperty() {
        return editando;
    }

    public BooleanBinding puedeGuardarProperty() {
        return puedeGuardar;
    }
}
