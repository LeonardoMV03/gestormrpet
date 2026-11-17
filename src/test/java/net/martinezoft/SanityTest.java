package net.martinezoft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SanityTest {

    @Test
    void debeEjecutarseSobreJava25OMayor() {
        assertTrue(Runtime.version().feature() >= 25,
                "El build requiere Java 25 o superior, runtime actual: " + Runtime.version());
    }

    @Test
    void debeCargarLaClasePrincipalDeLaAplicacion() {
        assertEquals("net.martinezoft.App", App.class.getName());
    }
}
