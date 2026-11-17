package net.martinezoft.data;

import java.nio.file.Path;

import org.flywaydb.core.Flyway;

/**
 * Aplica las migraciones versionadas de {@code classpath:db/migration}. Es
 * idempotente: Flyway solo ejecuta las versiones pendientes.
 */
public final class DatabaseInitializer {

    private static final String UBICACIONES = "classpath:db/migration";

    private final Database database;

    public DatabaseInitializer(Database database) {
        this.database = database;
    }

    public DatabaseInitializer(Path rutaArchivo) {
        this(new Database(rutaArchivo));
    }

    public void migrar() {
        Flyway.configure()
                .dataSource(database.url(), "", "")
                .locations(UBICACIONES)
                .load()
                .migrate();
    }
}
