package net.martinezoft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Valida la base de pruebas de F0.3: versiones alineadas y dependencias de test
 * realmente disponibles en el classpath (surefire con useModulePath=false).
 */
class StackDePruebasTest {

    @Test
    void debeResolverJavaFx27EnElClasspath() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream("javafx.properties")) {
            assertNotNull(entrada, "No se encontró javafx.properties en el classpath");
            propiedades.load(entrada);
        }
        assertEquals("27", propiedades.getProperty("javafx.version"),
                "JavaFX debe estar alineado a la versión 27");
    }

    @Test
    void debeTenerTestFxJUnit5DisponibleEnElClasspath() {
        assertNotNull(recurso("org/testfx/framework/junit5/ApplicationExtension.class"),
                "testfx-junit5 no está en el classpath de pruebas");
    }

    @Test
    void debeTenerMonocleHeadlessDisponibleEnElClasspath() {
        assertNotNull(recurso("com/sun/glass/ui/monocle/HeadlessPlatformFactory.class"),
                "openjfx-monocle no está en el classpath de pruebas");
    }

    @Test
    void debeEjecutarLasPruebasEnClasspathYNoEnModulePath() {
        assertFalse(SanityTest.class.getModule().isNamed(),
                "surefire debe ejecutar los tests en classpath (useModulePath=false)");
    }

    private static java.net.URL recurso(String nombre) {
        return StackDePruebasTest.class.getClassLoader().getResource(nombre);
    }
}
