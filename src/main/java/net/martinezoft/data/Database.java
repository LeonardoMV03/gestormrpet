package net.martinezoft.data;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Punto único de apertura de conexiones SQLite. Cada conexión aplica los
 * PRAGMAs necesarios para integridad referencial, concurrencia y WAL.
 */
public final class Database {

    private static final int BUSY_TIMEOUT_MS = 5000;

    private final String url;

    private Database(String url) {
        this.url = url;
    }

    public Database(Path rutaArchivo) {
        this("jdbc:sqlite:" + Objects.requireNonNull(rutaArchivo, "La ruta de la BD es obligatoria")
                .toAbsolutePath().normalize());
    }

    public static Database deUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL JDBC es obligatoria");
        }
        return new Database(url);
    }

    public String url() {
        return url;
    }

    /**
     * Abre una conexión nueva con {@code foreign_keys=ON}, {@code journal_mode=WAL}
     * y {@code busy_timeout=5000}.
     */
    public Connection conexion() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try {
            aplicarPragmas(conexion);
            return conexion;
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
    }

    private static void aplicarPragmas(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("PRAGMA foreign_keys = ON");
            sentencia.execute("PRAGMA journal_mode = WAL");
            sentencia.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
        }
    }
}
