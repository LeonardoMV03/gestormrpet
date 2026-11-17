package net.martinezoft.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.domain.Usuario;

/**
 * Sesión en memoria del usuario autenticado. Expira a las 12 horas de haberse
 * iniciado y nunca se persiste en disco.
 */
public final class SesionManager {

    public static final Duration DURACION = Duration.ofHours(12);

    private final Clock reloj;
    private Sesion sesion;

    public SesionManager() {
        this(Clock.systemUTC());
    }

    public SesionManager(Clock reloj) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio");
    }

    public synchronized Sesion iniciar(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario es obligatorio");
        Instant inicio = reloj.instant();
        Sesion nueva = new Sesion(usuario, inicio, inicio.plus(DURACION));
        this.sesion = nueva;
        return nueva;
    }

    /**
     * Devuelve la sesión vigente. Si expiró, la descarta y devuelve vacío.
     */
    public synchronized Optional<Sesion> actual() {
        if (sesion == null) {
            return Optional.empty();
        }
        if (!reloj.instant().isBefore(sesion.expira())) {
            sesion = null;
            return Optional.empty();
        }
        return Optional.of(sesion);
    }

    public synchronized boolean activa() {
        return actual().isPresent();
    }

    public synchronized void cerrar() {
        sesion = null;
    }

    public record Sesion(Usuario usuario, Instant inicio, Instant expira) {
        public Sesion {
            Objects.requireNonNull(usuario, "El usuario es obligatorio");
            Objects.requireNonNull(inicio, "El inicio es obligatorio");
            Objects.requireNonNull(expira, "La expiración es obligatoria");
        }
    }
}
