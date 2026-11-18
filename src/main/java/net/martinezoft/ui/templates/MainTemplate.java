package net.martinezoft.ui.templates;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

        sidebar.setOnCerrarSesion(() -> {
            vm.cerrarSesion();
            this.alCerrarSesion.run();
        });
    }

    /**
     * Declara los destinos del menú y avisa cada vez que cambia la selección.
     * El template no conoce las páginas: solo entrega el destino elegido.
     */
    public void configurarDestinos(List<SidebarNav.Destino> destinos,
                                   SidebarNav.Destino inicial,
                                   Consumer<SidebarNav.Destino> alNavegar) {
        Objects.requireNonNull(destinos, "Los destinos son obligatorios");
        Objects.requireNonNull(inicial, "El destino inicial es obligatorio");
        Objects.requireNonNull(alNavegar, "La acción de navegación es obligatoria");
        sidebar.seleccionadoProperty().addListener((observable, anterior, actual) -> {
            if (actual != null) {
                alNavegar.accept(actual);
            }
        });
        sidebar.getDestinos().setAll(destinos);
        sidebar.setSeleccionado(inicial);
    }

    public void setContenido(Node nodo) {
        contenido.getChildren().setAll(nodo);
    }
}
