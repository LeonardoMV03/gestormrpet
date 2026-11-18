package net.martinezoft.ui;

import java.io.IOException;
import java.io.UncheckedIOException;

import javafx.fxml.FXMLLoader;

/**
 * Carga el FXML de un componente con el patrón {@code fx:root}: la propia
 * instancia es la raíz visual y el controller a la vez. El FXML se localiza
 * junto a la clase con el mismo nombre.
 */
public final class CargadorFxml {

    private CargadorFxml() {
    }

    public static void cargar(Object instancia, Class<?> clase) {
        FXMLLoader cargador = new FXMLLoader(clase.getResource(clase.getSimpleName() + ".fxml"));
        cargador.setRoot(instancia);
        cargador.setController(instancia);
        try {
            cargador.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista de " + clase.getSimpleName(), e);
        }
    }
}
