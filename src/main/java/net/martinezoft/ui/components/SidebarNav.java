package net.martinezoft.ui.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;

/**
 * Menú lateral. Los destinos se declaran desde fuera y la selección queda
 * expuesta como propiedad observable; la navegación real la hace la página.
 */
public class SidebarNav extends VBox {

    public record Destino(String clave, String etiqueta) {
    }

    private static final String CLASE_ACTIVO = "activo";

    @FXML
    private VBox listaDestinos;

    private final ObservableList<Destino> destinos = FXCollections.observableArrayList();
    private final ObjectProperty<Destino> seleccionado = new SimpleObjectProperty<>();
    private final Map<Destino, Button> botones = new HashMap<>();
    private Runnable alCerrarSesion = () -> { };

    public SidebarNav() {
        CargadorFxml.cargar(this, SidebarNav.class);
        destinos.addListener((InvalidationListener) observable -> reconstruirBotones());
        seleccionado.addListener((observable, anterior, actual) -> marcarActivo());

        Button botonCerrarSesion = new Button("Cerrar sesión");
        botonCerrarSesion.setMaxWidth(Double.MAX_VALUE);
        botonCerrarSesion.setOnAction(e -> alCerrarSesion.run());
        getChildren().add(botonCerrarSesion);
    }

    public void setOnCerrarSesion(Runnable accion) {
        this.alCerrarSesion = Objects.requireNonNull(accion, "La acción es obligatoria");
    }

    public ObservableList<Destino> getDestinos() {
        return destinos;
    }

    public ObjectProperty<Destino> seleccionadoProperty() {
        return seleccionado;
    }

    public Destino getSeleccionado() {
        return seleccionado.get();
    }

    public void setSeleccionado(Destino destino) {
        seleccionado.set(destino);
    }

    private void reconstruirBotones() {
        listaDestinos.getChildren().clear();
        botones.clear();
        for (Destino destino : destinos) {
            Button boton = new Button(destino.etiqueta());
            boton.getStyleClass().add("boton-navegacion");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setOnAction(e -> setSeleccionado(destino));
            botones.put(destino, boton);
            listaDestinos.getChildren().add(boton);
        }
        marcarActivo();
    }

    private void marcarActivo() {
        botones.forEach((destino, boton) -> {
            boolean activo = destino.equals(seleccionado.get());
            boton.getStyleClass().remove(CLASE_ACTIVO);
            if (activo) {
                boton.getStyleClass().add(CLASE_ACTIVO);
            }
        });
    }
}
