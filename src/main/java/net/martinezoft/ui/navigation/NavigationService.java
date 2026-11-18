package net.martinezoft.ui.navigation;

import java.util.List;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.martinezoft.di.AppContext;
import net.martinezoft.ui.components.SidebarNav;
import net.martinezoft.ui.pages.ClientesPage;
import net.martinezoft.ui.pages.InicioPage;
import net.martinezoft.ui.pages.LoginPage;
import net.martinezoft.ui.pages.ServiciosPage;
import net.martinezoft.ui.templates.MainTemplate;
import net.martinezoft.ui.viewmodels.ClientesViewModel;
import net.martinezoft.ui.viewmodels.LoginViewModel;
import net.martinezoft.ui.viewmodels.MascotasViewModel;
import net.martinezoft.ui.viewmodels.ServiciosViewModel;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Único punto que cambia la escena de la ventana. Crea cada ViewModel, crea la
 * vista con él y la muestra; el enlace entre ambos lo hace la propia vista.
 */
public final class NavigationService {

    static final String TITULO = "gestormrpet";
    private static final double ANCHO = 960;
    private static final double ALTO = 640;

    private final AppContext contexto;
    private final Scene escena;
    private SesionWatcher vigilante;

    public NavigationService(Stage stage, AppContext contexto) {
        Objects.requireNonNull(stage, "El escenario es obligatorio");
        this.contexto = Objects.requireNonNull(contexto, "El contexto es obligatorio");
        this.escena = new Scene(new StackPane(), ANCHO, ALTO);
        this.escena.getStylesheets().addAll(
                NavigationService.class.getResource("/net/martinezoft/ui/design/tokens.css").toExternalForm(),
                NavigationService.class.getResource("/net/martinezoft/ui/design/theme.css").toExternalForm());
        stage.setTitle(TITULO);
        stage.setScene(escena);
    }

    public void mostrarLogin() {
        detenerVigilancia();
        LoginViewModel login = new LoginViewModel(contexto.auth());
        escena.setRoot(new LoginPage(login, this::mostrarShell));
    }

    public void mostrarShell() {
        detenerVigilancia();
        ShellViewModel shell = new ShellViewModel(contexto.sesion());
        MainTemplate plantilla = new MainTemplate(shell, this::mostrarLogin);

        InicioPage inicio = new InicioPage(shell);
        ClientesPage clientes = new ClientesPage(new ClientesViewModel(contexto.clientes()),
                new MascotasViewModel(contexto.mascotas()));
        ServiciosPage servicios = new ServiciosPage(new ServiciosViewModel(contexto.servicios()));

        SidebarNav.Destino destinoInicio = new SidebarNav.Destino("inicio", "Inicio");
        SidebarNav.Destino destinoClientes = new SidebarNav.Destino("clientes", "Clientes");
        SidebarNav.Destino destinoServicios = new SidebarNav.Destino("servicios", "Servicios");
        plantilla.configurarDestinos(List.of(destinoInicio, destinoClientes, destinoServicios), destinoInicio,
                destino -> plantilla.setContenido(contenidoDe(destino, inicio, clientes, servicios)));

        escena.setRoot(plantilla);

        vigilante = new SesionWatcher(shell, this::mostrarLogin);
        vigilante.iniciar();
    }

    private static Node contenidoDe(SidebarNav.Destino destino,
                                                 InicioPage inicio,
                                                 ClientesPage clientes,
                                                 ServiciosPage servicios) {
        return switch (destino.clave()) {
            case "clientes" -> clientes;
            case "servicios" -> servicios;
            default -> inicio;
        };
    }

    private void detenerVigilancia() {
        if (vigilante != null) {
            vigilante.detener();
            vigilante = null;
        }
    }
}
