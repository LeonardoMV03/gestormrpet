package net.martinezoft.ui.pages;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Página de inicio del shell. Placeholder hasta que existan las pantallas de negocio.
 */
public class InicioPage extends VBox {

    @FXML
    private Label usuario;

    public InicioPage(ShellViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel del shell es obligatorio");
        CargadorFxml.cargar(this, InicioPage.class);
        usuario.textProperty().bind(Bindings.concat("Sesión iniciada como ", vm.usuarioActualProperty()));
    }
}
