package net.martinezoft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import javafx.application.Application;

class SanityTest {

    @Test
    void debeEjecutarseSobreJava25OMayor() {
        assertTrue(Runtime.version().feature() >= 25,
                "El build requiere Java 25 o superior, runtime actual: " + Runtime.version());
    }

    @Test
    void debeCargarUnaAplicacionJavaFxConPuntoDeEntrada() throws NoSuchMethodException {
        assertTrue(Application.class.isAssignableFrom(App.class),
                "App debe extender javafx.application.Application");

        var main = App.class.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(main.getModifiers()),
                "App debe exponer un método main estático como punto de entrada");
    }
}
