package net.martinezoft.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolución de rutas de la aplicación. La base de datos nunca vive dentro del
 * repositorio ni de OneDrive; por defecto se guarda en el directorio de datos
 * del usuario.
 */
public final class AppConfig {

    public static final String PROPIEDAD_RUTA_BD = "gestormrpet.db";

    private static final String DIRECTORIO_APP = "gestormrpet";
    private static final String ARCHIVO_BD = "gestormrpet.db";

    private AppConfig() {
    }

    /**
     * Devuelve la ruta de la base de datos y garantiza que su directorio padre
     * exista. Prioridad:
     * <ol>
     *   <li>{@code -Dgestormrpet.db=<ruta>}</li>
     *   <li>{@code %APPDATA%\gestormrpet\gestormrpet.db}</li>
     *   <li>{@code ~/.gestormrpet/gestormrpet.db}</li>
     * </ol>
     */
    public static Path rutaBdPorDefecto() {
        Path ruta = rutaConfigurada();
        crearDirectorioPadre(ruta);
        return ruta.toAbsolutePath().normalize();
    }

    private static Path rutaConfigurada() {
        String override = System.getProperty(PROPIEDAD_RUTA_BD);
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim());
        }

        String appData = System.getenv("APPDATA");
        Path directorio = appData != null && !appData.isBlank()
                ? Path.of(appData, DIRECTORIO_APP)
                : Path.of(System.getProperty("user.home"), "." + DIRECTORIO_APP);
        return directorio.resolve(ARCHIVO_BD);
    }

    private static void crearDirectorioPadre(Path ruta) {
        Path padre = ruta.toAbsolutePath().getParent();
        if (padre == null) {
            return;
        }
        try {
            Files.createDirectories(padre);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de la base de datos: " + padre, e);
        }
    }
}
