package net.martinezoft.ui.atoms;

import javafx.scene.control.Button;

/**
 * Botón base de la aplicación. Sin lógica: solo aplica la clase CSS de su variante.
 */
public class AppButton extends Button {

    public enum Variante {
        SECUNDARIA("boton-secundario"),
        PRIMARIA("boton-primario"),
        PELIGRO("boton-peligro"),
        NAVEGACION("boton-navegacion");

        private final String claseCss;

        Variante(String claseCss) {
            this.claseCss = claseCss;
        }

        public String claseCss() {
            return claseCss;
        }
    }

    public AppButton() {
        this("", Variante.SECUNDARIA);
    }

    public AppButton(String texto) {
        this(texto, Variante.SECUNDARIA);
    }

    public AppButton(String texto, Variante variante) {
        super(texto);
        getStyleClass().addAll("app-button", variante.claseCss());
    }
}
