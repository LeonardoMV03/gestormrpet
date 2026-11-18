package net.martinezoft.ui.templates;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.organisms.SidebarNav;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Layout principal: banner de aviso arriba, menú a la izquierda y slot de
 * contenido en el centro. Enlaza el aviso y el cierre de sesión con su ViewModel.
 */
public class MainTemplate extends BorderPane {

    private static final String CLAVE_INICIO = "inicio";
    private static final String ETIQUETA_INICIO = "Inicio";

    @FXML
    private VBox bannerAviso;
    @FXML
    private Label textoAviso;
    @FXML
    private SidebarNav sidebar;
    @FXML
    private StackPane contenido;

    private final Runnable alCerrarSesion;

    public MainTemplate(ShellViewModel vm, Runnable alCerrarSesion) {
        Objects.requireNonNull(vm, "El ViewModel del shell es obligatorio");
        this.alCerrarSesion = Objects.requireNonNull(alCerrarSesion, "La acción de cierre de sesión es obligatoria");
        CargadorFxml.cargar(this, MainTemplate.class);

        textoAviso.textProperty().bind(vm.avisoClaveProperty());
        bannerAviso.visibleProperty().bind(vm.avisoClaveVisibleProperty());
        bannerAviso.managedProperty().bind(vm.avisoClaveVisibleProperty());

        SidebarNav.Destino inicio = new SidebarNav.Destino(CLAVE_INICIO, ETIQUETA_INICIO);
        sidebar.getDestinos().setAll(inicio);
        sidebar.setSeleccionado(inicio);
        sidebar.setOnCerrarSesion(() -> {
            vm.cerrarSesion();
            this.alCerrarSesion.run();
        });
    }

    public void setContenido(Node nodo) {
        contenido.getChildren().setAll(nodo);
    }
}
