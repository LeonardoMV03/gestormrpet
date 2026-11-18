package net.martinezoft.ui.organisms;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.atoms.AppButton;
import net.martinezoft.ui.atoms.AppPasswordField;
import net.martinezoft.ui.atoms.AppTextField;
import net.martinezoft.ui.molecules.FormField;

/**
 * Formulario de acceso. Expone sus valores como propiedades observables para
 * que la página los enlace con su ViewModel; no conoce servicios ni validaciones.
 */
public class LoginForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;

    private final StringProperty usuario = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty puedeIniciar = new SimpleBooleanProperty(true);
    private final AppButton botonEntrar = new AppButton("Entrar", AppButton.Variante.PRIMARIA);
    private Runnable alIniciar = () -> { };

    public LoginForm() {
        CargadorFxml.cargar(this, LoginForm.class);

        AppTextField campoUsuario = new AppTextField();
        campoUsuario.setPromptText("admin");
        AppPasswordField campoPassword = new AppPasswordField();

        campoUsuario.textProperty().bindBidirectional(usuario);
        campoPassword.textProperty().bindBidirectional(password);

        FormField formUsuario = new FormField("Usuario", campoUsuario);
        FormField formPassword = new FormField("Contraseña", campoPassword);
        formPassword.errorProperty().bind(error);

        campos.getChildren().addAll(formUsuario, formPassword);

        botonEntrar.disableProperty().bind(puedeIniciar.not());
        botonEntrar.setDefaultButton(true);
        botonEntrar.setOnAction(e -> alIniciar.run());
        zonaAccion.getChildren().add(botonEntrar);
        campoPassword.setOnAction(e -> alIniciar.run());
    }

    public StringProperty usuarioProperty() {
        return usuario;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public BooleanProperty puedeIniciarProperty() {
        return puedeIniciar;
    }

    public void setOnIniciar(Runnable accion) {
        this.alIniciar = Objects.requireNonNull(accion, "La acción es obligatoria");
    }
}
