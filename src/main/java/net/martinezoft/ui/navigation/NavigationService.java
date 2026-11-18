package net.martinezoft.ui.navigation;

import java.util.Objects;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.martinezoft.di.AppContext;
import net.martinezoft.ui.organisms.LoginForm;
import net.martinezoft.ui.organisms.SidebarNav;
import net.martinezoft.ui.pages.InicioPage;
import net.martinezoft.ui.pages.LoginPage;
import net.martinezoft.ui.templates.MainTemplate;
import net.martinezoft.ui.viewmodels.LoginViewModel;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Único punto que cambia la vista de la ventana. Crea los ViewModels, enlaza
 * sus propiedades con los componentes y reacciona a sus estados. Los
 * ViewModels nunca invocan este servicio.
 */
public final class NavigationService {

    static final String TITULO = "gestormrpet";
    private static final double ANCHO = 960;
    private static final double ALTO = 640;
    private static final String CLAVE_INICIO = "inicio";
    private static final String ETIQUETA_INICIO = "Inicio";

    private final Stage stage;
    private final AppContext contexto;
    private final Scene escena;
    private SesionWatcher vigilante;

    public NavigationService(Stage stage, AppContext contexto) {
        this.stage = Objects.requireNonNull(stage, "El escenario es obligatorio");
        this.contexto = Objects.requireNonNull(contexto, "El contexto es obligatorio");
        this.escena = new Scene(new StackPane(), ANCHO, ALTO);
        this.escena.getStylesheets().addAll(
                NavigationService.class.getResource("/net/martinezoft/ui/design/tokens.css").toExternalForm(),
                NavigationService.class.getResource("/net/martinezoft/ui/design/theme.css").toExternalForm());
        this.stage.setTitle(TITULO);
        this.stage.setScene(escena);
    }

    public void mostrarLogin() {
        detenerVigilancia();
        LoginPage pagina = new LoginPage();
        LoginViewModel vm = new LoginViewModel(contexto.auth());
        enlazarLogin(pagina.getFormulario(), vm);
        vm.autenticadoProperty().addListener((observable, anterior, autenticado) -> {
            if (autenticado) {
                mostrarShell();
            }
        });
        escena.setRoot(pagina);
    }

    public void mostrarShell() {
        detenerVigilancia();
        ShellViewModel shell = new ShellViewModel(contexto.sesion());
        MainTemplate plantilla = new MainTemplate();
        plantilla.avisoProperty().bind(shell.avisoClaveProperty());
        plantilla.avisoVisibleProperty().bind(shell.avisoClaveVisibleProperty());
        configurarMenu(plantilla.getSidebar(), shell);
        plantilla.setContenido(new InicioPage());
        escena.setRoot(plantilla);

        vigilante = new SesionWatcher(shell, this::mostrarLogin);
        vigilante.iniciar();
    }

    private void enlazarLogin(LoginForm formulario, LoginViewModel vm) {
        formulario.usuarioProperty().bindBidirectional(vm.usuarioProperty());
        formulario.passwordProperty().bindBidirectional(vm.passwordProperty());
        formulario.errorProperty().bind(vm.errorProperty());
        formulario.puedeIniciarProperty().bind(vm.puedeIniciarProperty());
        formulario.setOnIniciar(vm::iniciarSesion);
    }

    private void configurarMenu(SidebarNav menu, ShellViewModel shell) {
        SidebarNav.Destino inicio = new SidebarNav.Destino(CLAVE_INICIO, ETIQUETA_INICIO);
        menu.getDestinos().setAll(inicio);
        menu.setSeleccionado(inicio);
        menu.setOnCerrarSesion(() -> {
            shell.cerrarSesion();
            mostrarLogin();
        });
    }

    private void detenerVigilancia() {
        if (vigilante != null) {
            vigilante.detener();
            vigilante = null;
        }
    }
}
