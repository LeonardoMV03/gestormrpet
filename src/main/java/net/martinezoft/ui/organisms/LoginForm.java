package net.martinezoft.ui.organisms;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.atoms.AppButton;
import net.martinezoft.ui.atoms.AppPasswordField;
import net.martinezoft.ui.atoms.AppTextField;
import net.martinezoft.ui.molecules.FormField;
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

    private final AppButton botonEntrar = new AppButton("Entrar", AppButton.Variante.PRIMARIA);

    public LoginForm(LoginViewModel vm) {
        Objects.requireNonNull(vm, "El ViewModel de login es obligatorio");
        CargadorFxml.cargar(this, LoginForm.class);

        AppTextField campoUsuario = new AppTextField();
        campoUsuario.setPromptText("admin");
        AppPasswordField campoPassword = new AppPasswordField();

        campoUsuario.textProperty().bindBidirectional(vm.usuarioProperty());
        campoPassword.textProperty().bindBidirectional(vm.passwordProperty());

        FormField formUsuario = new FormField("Usuario", campoUsuario);
        FormField formPassword = new FormField("Contraseña", campoPassword);
        formPassword.errorProperty().bind(vm.errorProperty());

        campos.getChildren().addAll(formUsuario, formPassword);

        botonEntrar.disableProperty().bind(vm.puedeIniciarProperty().not());
        botonEntrar.setDefaultButton(true);
        botonEntrar.setOnAction(e -> vm.iniciarSesion());
        zonaAccion.getChildren().add(botonEntrar);
        campoPassword.setOnAction(e -> vm.iniciarSesion());
    }
}
