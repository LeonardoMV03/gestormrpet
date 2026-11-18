package net.martinezoft.ui.viewmodels;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj cuyo instante avanza solo cuando el test lo indica, para probar la
 * expiración de sesión sin esperas reales.
 */
final class RelojMutable extends Clock {

    private Instant instante;

    RelojMutable(Instant inicio) {
        this.instante = inicio;
    }

    void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        throw new UnsupportedOperationException("No se usa en los tests");
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
