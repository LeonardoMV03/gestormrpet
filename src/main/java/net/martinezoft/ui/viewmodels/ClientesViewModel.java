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
import net.martinezoft.domain.Cliente;
import net.martinezoft.service.ClienteService;

/**
 * Estado y comandos de la pantalla de clientes: búsqueda, listado y formulario.
 * No conoce vistas ni el toolkit.
 */
public class ClientesViewModel {

    static final String MENSAJE_GUARDADO = "Cliente guardado";
    static final String MENSAJE_DESACTIVADO = "Cliente desactivado";
    static final String MENSAJE_ERROR_CARGA = "No se pudieron cargar los clientes";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo completar la operación, intenta de nuevo";
    static final String MENSAJE_SIN_SELECCION = "Selecciona un cliente para desactivarlo";

    private final ClienteService clienteService;
    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private final ObjectProperty<Cliente> seleccionado = new SimpleObjectProperty<>();

    private final StringProperty busqueda = new SimpleStringProperty("");
    private final StringProperty nombre = new SimpleStringProperty("");
    private final StringProperty telefono = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty direccion = new SimpleStringProperty("");
    private final StringProperty notas = new SimpleStringProperty("");

    private final StringProperty errorNombre = new SimpleStringProperty("");
    private final StringProperty errorGeneral = new SimpleStringProperty("");
    private final StringProperty mensaje = new SimpleStringProperty("");
    private final BooleanProperty editando = new SimpleBooleanProperty(false);

    private final BooleanBinding puedeGuardar;
    private boolean sincronizando;

    public ClientesViewModel(ClienteService clienteService) {
        this.clienteService = Objects.requireNonNull(clienteService,
                "El servicio de clientes es obligatorio");
        this.puedeGuardar = Bindings.createBooleanBinding(
                () -> !textoVacio(nombre.get()), nombre);
        seleccionado.addListener((observable, anterior, actual) -> {
            if (!sincronizando) {
                volcarFormulario(actual);
            }
        });
        busqueda.addListener((observable, anterior, actual) -> recargar());
        nombre.addListener((observable, anterior, actual) -> {
            if (!textoVacio(actual)) {
                errorNombre.set("");
            }
        });
    }

    public void cargar() {
        limpiarErrores();
        mensaje.set("");
        recargar();
    }

    public void recargar() {
        Long idSeleccionado = seleccionado.get() == null ? null : seleccionado.get().id();
        try {
            List<Cliente> encontrados = clienteService.buscar(busqueda.get());
            clientes.setAll(encontrados);
            seleccionado.set(encontrados.stream()
                    .filter(cliente -> Objects.equals(cliente.id(), idSeleccionado))
                    .findFirst()
                    .orElse(null));
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

    public void seleccionar(Cliente cliente) {
        seleccionado.set(cliente);
    }

    public void guardar() {
        limpiarErrores();
        mensaje.set("");
        try {
            String nombreLimpio = nombre.get().trim();
            Cliente actual = seleccionado.get();
            Cliente guardado;
            if (editando.get() && actual != null && actual.id() != null) {
                guardado = clienteService.actualizar(new Cliente(actual.id(), nombreLimpio,
                        telefono.get(), email.get(), direccion.get(), notas.get(), true));
            } else {
                guardado = clienteService.crear(Cliente.nuevo(nombreLimpio, telefono.get(),
                        email.get(), direccion.get(), notas.get()));
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
        Cliente actual = seleccionado.get();
        if (actual == null || actual.id() == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        try {
            clienteService.desactivar(actual.id());
            sincronizando = true;
            try {
                seleccionado.set(null);
            } finally {
                sincronizando = false;
            }
            limpiarCampos();
            recargar();
            mensaje.set(MENSAJE_DESACTIVADO);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    private void recargarSeleccionando(Long id) throws SQLException {
        List<Cliente> encontrados = clienteService.buscar(busqueda.get());
        clientes.setAll(encontrados);
        Cliente encontrado = encontrados.stream()
                .filter(cliente -> Objects.equals(cliente.id(), id))
                .findFirst()
                .orElse(null);
        sincronizando = true;
        try {
            seleccionado.set(encontrado);
        } finally {
            sincronizando = false;
        }
        volcarFormulario(encontrado);
    }

    private void volcarFormulario(Cliente cliente) {
        limpiarErrores();
        mensaje.set("");
        if (cliente == null) {
            limpiarCampos();
            return;
        }
        nombre.set(cliente.nombre());
        telefono.set(valor(cliente.telefono()));
        email.set(valor(cliente.email()));
        direccion.set(valor(cliente.direccion()));
        notas.set(valor(cliente.notas()));
        editando.set(true);
    }

    private void limpiarCampos() {
        nombre.set("");
        telefono.set("");
        email.set("");
        direccion.set("");
        notas.set("");
        editando.set(false);
    }

    private void aplicarError(IllegalArgumentException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if (texto.startsWith("El nombre")) {
            errorNombre.set(texto);
        } else {
            errorGeneral.set(texto);
        }
    }

    private void limpiarErrores() {
        errorNombre.set("");
        errorGeneral.set("");
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public ObservableList<Cliente> clientes() {
        return clientes;
    }

    public ObjectProperty<Cliente> seleccionadoProperty() {
        return seleccionado;
    }

    public StringProperty busquedaProperty() {
        return busqueda;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public StringProperty telefonoProperty() {
        return telefono;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty direccionProperty() {
        return direccion;
    }

    public StringProperty notasProperty() {
        return notas;
    }

    public StringProperty errorNombreProperty() {
        return errorNombre;
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
