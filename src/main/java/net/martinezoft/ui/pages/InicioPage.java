package net.martinezoft.ui.pages;

import javafx.scene.layout.VBox;
import net.martinezoft.ui.CargadorFxml;

/**
 * Página de inicio del shell. Placeholder hasta que existan las pantallas de negocio.
 */
public class InicioPage extends VBox {

    public InicioPage() {
        CargadorFxml.cargar(this, InicioPage.class);
    }
}
