package net.martinezoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    @TempDir
    Path tempDir;

    private String valorPrevio;

    @BeforeEach
    void guardarPropiedad() {
        valorPrevio = System.getProperty(AppConfig.PROPIEDAD_RUTA_BD);
    }

    @AfterEach
    void restaurarPropiedad() {
        if (valorPrevio == null) {
            System.clearProperty(AppConfig.PROPIEDAD_RUTA_BD);
        } else {
            System.setProperty(AppConfig.PROPIEDAD_RUTA_BD, valorPrevio);
        }
    }

    @Test
    void debeRespetarRutaExplicitaYCrearDirectorioPadre() {
        Path archivo = tempDir.resolve("datos").resolve("prueba.db");
        System.setProperty(AppConfig.PROPIEDAD_RUTA_BD, archivo.toString());

        Path ruta = AppConfig.rutaBdPorDefecto();

        assertEquals(archivo.toAbsolutePath().normalize(), ruta);
        assertTrue(Files.isDirectory(archivo.getParent()));
    }

    @Test
    void noDebeCrearLaBaseDeDatosSoloAlResolverLaRuta() {
        Path archivo = tempDir.resolve("solo-config").resolve("prueba.db");
        System.setProperty(AppConfig.PROPIEDAD_RUTA_BD, archivo.toString());

        AppConfig.rutaBdPorDefecto();

        assertFalse(Files.exists(archivo), "Resolver la ruta no debe crear el archivo de BD");
    }

    @Test
    void debeRecortarEspaciosDelOverride() {
        Path archivo = tempDir.resolve("con-espacios").resolve("prueba.db");
        System.setProperty(AppConfig.PROPIEDAD_RUTA_BD, "  " + archivo + "  ");

        assertEquals(archivo.toAbsolutePath().normalize(), AppConfig.rutaBdPorDefecto());
    }
}
