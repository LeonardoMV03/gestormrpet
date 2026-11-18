package net.martinezoft.di;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

import net.martinezoft.config.SesionManager;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.data.ServicioRepository;
import net.martinezoft.data.UsuarioRepository;
import net.martinezoft.security.PasswordHasher;
import net.martinezoft.service.AuthService;
import net.martinezoft.service.ServicioService;

/**
 * Composition root: construye y conecta las dependencias de la aplicación.
 */
public final class AppContext {

    private final Database database;
    private final UsuarioRepository usuarios;
    private final ServicioRepository servicioRepository;
    private final PasswordHasher hasher;
    private final SesionManager sesion;
    private final AuthService auth;
    private final ServicioService servicios;

    private AppContext(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
        this.usuarios = new UsuarioRepository(database);
        this.servicioRepository = new ServicioRepository(database);
        this.hasher = new PasswordHasher();
        this.sesion = new SesionManager();
        this.auth = new AuthService(usuarios, hasher, sesion);
        this.servicios = new ServicioService(servicioRepository);
    }

    public static AppContext crear(Path rutaBd) {
        return new AppContext(new Database(rutaBd));
    }

    public static AppContext crear(Database database) {
        return new AppContext(database);
    }

    /**
     * Aplica las migraciones pendientes y garantiza el usuario {@code admin}.
     */
    public void inicializarBaseDatos() throws SQLException {
        new DatabaseInitializer(database).migrar();
        auth.asegurarAdmin();
    }

    public Database database() {
        return database;
    }

    public UsuarioRepository usuarios() {
        return usuarios;
    }

    public PasswordHasher hasher() {
        return hasher;
    }

    public SesionManager sesion() {
        return sesion;
    }

    public AuthService auth() {
        return auth;
    }

    public ServicioService servicios() {
        return servicios;
    }
}
