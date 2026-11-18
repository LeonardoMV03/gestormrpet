package net.martinezoft.ui.viewmodels;

import java.sql.SQLException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.martinezoft.domain.Mascota;
import net.martinezoft.service.MascotaService;

/**
 * Estado y comandos de las mascotas del cliente seleccionado. No conoce vistas
 * ni el toolkit.
 */
public class MascotasViewModel {

    static final String MENSAJE_GUARDADO = "Mascota guardada";
    static final String MENSAJE_DESACTIVADA = "Mascota desactivada";
    static final String MENSAJE_ERROR_CARGA = "No se pudieron cargar las mascotas";
    static final String MENSAJE_ERROR_TECNICO = "No se pudo completar la operación, intenta de nuevo";
    static final String MENSAJE_SIN_CLIENTE = "Selecciona un cliente para gestionar sus mascotas";
    static final String MENSAJE_SIN_SELECCION = "Selecciona una mascota para desactivarla";

    private final MascotaService mascotaService;
    private final ObservableList<Mascota> mascotas = FXCollections.observableArrayList();
    private final ObjectProperty<Mascota> seleccionada = new SimpleObjectProperty<>();
    private final LongProperty clienteId = new SimpleLongProperty();

    private final StringProperty nombre = new SimpleStringProperty("");
    private final StringProperty especie = new SimpleStringProperty("");
    private final StringProperty raza = new SimpleStringProperty("");
    private final StringProperty sexo = new SimpleStringProperty("");
    private final StringProperty fechaNacimiento = new SimpleStringProperty("");
    private final StringProperty notas = new SimpleStringProperty("");

    private final StringProperty errorNombre = new SimpleStringProperty("");
    private final StringProperty errorFecha = new SimpleStringProperty("");
    private final StringProperty errorGeneral = new SimpleStringProperty("");
    private final StringProperty mensaje = new SimpleStringProperty("");
    private final BooleanProperty editando = new SimpleBooleanProperty(false);

    private final BooleanBinding puedeGuardar;
    private boolean sincronizando;

    public MascotasViewModel(MascotaService mascotaService) {
        this.mascotaService = Objects.requireNonNull(mascotaService,
                "El servicio de mascotas es obligatorio");
        this.puedeGuardar = Bindings.createBooleanBinding(
                () -> clienteId.get() > 0 && !textoVacio(nombre.get()), nombre, clienteId);
        seleccionada.addListener((observable, anterior, actual) -> {
            if (!sincronizando) {
                volcarFormulario(actual);
            }
        });
        nombre.addListener((observable, anterior, actual) -> {
            if (!textoVacio(actual)) {
                errorNombre.set("");
            }
        });
        fechaNacimiento.addListener((observable, anterior, actual) -> {
            if (textoVacio(actual)) {
                errorFecha.set("");
            }
        });
    }

    public void cargarPara(Long cliente) {
        limpiarErrores();
        mensaje.set("");
        Long anterior = clienteId.get() == 0 ? null : clienteId.get();
        clienteId.set(cliente == null ? 0 : cliente);
        if (!Objects.equals(anterior, cliente)) {
            sincronizando = true;
            try {
                seleccionada.set(null);
            } finally {
                sincronizando = false;
            }
            limpiarCampos();
        }
        recargar();
    }

    public void recargar() {
        if (clienteId.get() <= 0) {
            mascotas.clear();
            return;
        }
        Long idSeleccionada = seleccionada.get() == null ? null : seleccionada.get().id();
        try {
            mascotas.setAll(mascotaService.listarPorCliente(clienteId.get()));
            seleccionada.set(mascotas.stream()
                    .filter(mascota -> Objects.equals(mascota.id(), idSeleccionada))
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
            seleccionada.set(null);
        } finally {
            sincronizando = false;
        }
        limpiarCampos();
    }

    public void seleccionar(Mascota mascota) {
        seleccionada.set(mascota);
    }

    public void guardar() {
        limpiarErrores();
        mensaje.set("");
        if (clienteId.get() <= 0) {
            errorGeneral.set(MENSAJE_SIN_CLIENTE);
            return;
        }
        try {
            Mascota actual = seleccionada.get();
            Mascota guardada;
            if (editando.get() && actual != null && actual.id() != null) {
                guardada = mascotaService.actualizar(new Mascota(actual.id(), clienteId.get(),
                        nombre.get().trim(), especie.get(), raza.get(), sexo.get(),
                        fechaNacimiento.get(), notas.get(), true));
            } else {
                guardada = mascotaService.crear(Mascota.nueva(clienteId.get(), nombre.get().trim(),
                        especie.get(), raza.get(), sexo.get(), fechaNacimiento.get(), notas.get()));
            }
            recargarSeleccionando(guardada.id());
            mensaje.set(MENSAJE_GUARDADO);
        } catch (IllegalArgumentException e) {
            aplicarError(e);
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    public void desactivarSeleccionada() {
        limpiarErrores();
        mensaje.set("");
        Mascota actual = seleccionada.get();
        if (actual == null || actual.id() == null) {
            errorGeneral.set(MENSAJE_SIN_SELECCION);
            return;
        }
        try {
            mascotaService.desactivar(actual.id());
            sincronizando = true;
            try {
                seleccionada.set(null);
            } finally {
                sincronizando = false;
            }
            limpiarCampos();
            recargar();
            mensaje.set(MENSAJE_DESACTIVADA);
        } catch (IllegalArgumentException e) {
            errorGeneral.set(e.getMessage());
        } catch (SQLException e) {
            errorGeneral.set(MENSAJE_ERROR_TECNICO);
        }
    }

    private void recargarSeleccionando(Long id) throws SQLException {
        mascotas.setAll(mascotaService.listarPorCliente(clienteId.get()));
        Mascota encontrada = mascotas.stream()
                .filter(mascota -> Objects.equals(mascota.id(), id))
                .findFirst()
                .orElse(null);
        sincronizando = true;
        try {
            seleccionada.set(encontrada);
        } finally {
            sincronizando = false;
        }
        volcarFormulario(encontrada);
    }

    private void volcarFormulario(Mascota mascota) {
        limpiarErrores();
        mensaje.set("");
        if (mascota == null) {
            limpiarCampos();
            return;
        }
        nombre.set(mascota.nombre());
        especie.set(valor(mascota.especie()));
        raza.set(valor(mascota.raza()));
        sexo.set(valor(mascota.sexo()));
        fechaNacimiento.set(valor(mascota.fechaNacimiento()));
        notas.set(valor(mascota.notas()));
        editando.set(true);
    }

    private void limpiarCampos() {
        nombre.set("");
        especie.set("");
        raza.set("");
        sexo.set("");
        fechaNacimiento.set("");
        notas.set("");
        editando.set(false);
    }

    private void aplicarError(IllegalArgumentException e) {
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if (texto.startsWith("El nombre de la mascota")) {
            errorNombre.set(texto);
        } else if (texto.startsWith("La fecha")) {
            errorFecha.set(texto);
        } else {
            errorGeneral.set(texto);
        }
    }

    private void limpiarErrores() {
        errorNombre.set("");
        errorFecha.set("");
        errorGeneral.set("");
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public ObservableList<Mascota> mascotas() {
        return mascotas;
    }

    public ObjectProperty<Mascota> seleccionadaProperty() {
        return seleccionada;
    }

    public boolean tieneCliente() {
        return clienteId.get() > 0;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public StringProperty especieProperty() {
        return especie;
    }

    public StringProperty razaProperty() {
        return raza;
    }

    public StringProperty sexoProperty() {
        return sexo;
    }

    public StringProperty fechaNacimientoProperty() {
        return fechaNacimiento;
    }

    public StringProperty notasProperty() {
        return notas;
    }

    public StringProperty errorNombreProperty() {
        return errorNombre;
    }

    public StringProperty errorFechaProperty() {
        return errorFecha;
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
