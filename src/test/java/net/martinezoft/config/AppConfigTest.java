package net.martinezoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    @TempDir
    Path tempDir;

    private String valorPrevio;

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
        valorPrevio = System.getProperty(AppConfig.PROPIEDAD_RUTA_BD);
        Path archivo = tempDir.resolve("datos").resolve("prueba.db");
        System.setProperty(AppConfig.PROPIEDAD_RUTA_BD, archivo.toString());

        Path ruta = AppConfig.rutaBdPorDefecto();

        assertEquals(archivo.toAbsolutePath().normalize(), ruta);
        assertTrue(Files.isDirectory(archivo.getParent()));
    }
}
