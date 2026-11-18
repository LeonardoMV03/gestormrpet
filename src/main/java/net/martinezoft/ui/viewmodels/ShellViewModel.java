package net.martinezoft.ui.viewmodels;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.martinezoft.config.SesionManager;

/**
 * Estado del shell principal a partir de la sesión activa. La sesión vive en
 * {@link SesionManager}; este ViewModel solo la consulta y la cierra.
 */
public class ShellViewModel {

    static final String MENSAJE_AVISO_CLAVE =
            "Estás usando la contraseña por defecto. Cámbiala desde Ajustes.";

    private final SesionManager sesion;
    private final StringProperty usuarioActual = new SimpleStringProperty("");
    private final StringProperty avisoClave = new SimpleStringProperty("");
    private final BooleanProperty avisoClaveVisible = new SimpleBooleanProperty(false);

    public ShellViewModel(SesionManager sesion) {
        this.sesion = Objects.requireNonNull(sesion, "El gestor de sesión es obligatorio");
        sesion.actual().ifPresent(vigente -> {
            boolean debeCambiar = vigente.usuario().debeCambiarPassword();
            usuarioActual.set(vigente.usuario().usuario());
            avisoClaveVisible.set(debeCambiar);
            avisoClave.set(debeCambiar ? MENSAJE_AVISO_CLAVE : "");
        });
    }

    public boolean sesionVigente() {
        return sesion.activa();
    }

    public void cerrarSesion() {
        sesion.cerrar();
    }

    public StringProperty usuarioActualProperty() {
        return usuarioActual;
    }

    public StringProperty avisoClaveProperty() {
        return avisoClave;
    }

    public BooleanProperty avisoClaveVisibleProperty() {
        return avisoClaveVisible;
    }
}
