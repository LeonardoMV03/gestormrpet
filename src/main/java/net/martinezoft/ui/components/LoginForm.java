package net.martinezoft.ui.components;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.viewmodels.LoginViewModel;

/**
 * Formulario de acceso. Enlaza sus controles con el {@link LoginViewModel}
 * recibido por constructor; no conoce servicios ni validaciones.
 */
public class LoginForm extends VBox {

    @FXML
    private VBox campos;
    @FXML
    private HBox zonaAccion;

    public LoginForm(LoginViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de login es obligatorio");
        CargadorFxml.cargar(this, LoginForm.class);

        TextField campoUsuario = new TextField();
        campoUsuario.setPromptText("admin");
        PasswordField campoPassword = new PasswordField();

        campoUsuario.textProperty().bindBidirectional(vm.usuarioProperty());
        campoPassword.textProperty().bindBidirectional(vm.passwordProperty());

        FormField formUsuario = new FormField("Usuario", campoUsuario);
        FormField formPassword = new FormField("Contraseña", campoPassword);
        formPassword.errorProperty().bind(vm.errorProperty());

        campos.getChildren().addAll(formUsuario, formPassword);

        Button botonEntrar = new Button("Entrar");
        botonEntrar.getStyleClass().add("boton-primario");
        botonEntrar.disableProperty().bind(vm.puedeIniciarProperty().not());
        botonEntrar.setDefaultButton(true);
        botonEntrar.setOnAction(e -> vm.iniciarSesion());
        zonaAccion.getChildren().add(botonEntrar);
        campoPassword.setOnAction(e -> vm.iniciarSesion());
    }
}
