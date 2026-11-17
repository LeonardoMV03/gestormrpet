package net.martinezoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import net.martinezoft.config.SesionManager.Sesion;
import net.martinezoft.domain.Usuario;

class SesionManagerTest {

    private static final Usuario ADMIN = new Usuario(1L, "admin", "hash", false);
    private static final Instant INICIO = Instant.parse("2026-11-17T08:00:00Z");

    @Test
    void noDebeHaberSesionAntesDeIniciar() {
        SesionManager manager = new SesionManager(new RelojMutable(INICIO));

        assertTrue(manager.actual().isEmpty());
        assertFalse(manager.activa());
    }

    @Test
    void debeIniciarUnaSesionDeDoceHoras() {
        SesionManager manager = new SesionManager(new RelojMutable(INICIO));

        Sesion sesion = manager.iniciar(ADMIN);

        assertEquals(INICIO, sesion.inicio());
        assertEquals(INICIO.plus(Duration.ofHours(12)), sesion.expira());
        assertEquals(ADMIN, sesion.usuario());
        assertTrue(manager.activa());
        assertEquals(sesion, manager.actual().orElseThrow());
    }

    @Test
    void debeSeguirActivaJustoAntesDeLasDoceHoras() {
        RelojMutable reloj = new RelojMutable(INICIO);
        SesionManager manager = new SesionManager(reloj);
        manager.iniciar(ADMIN);

        reloj.avanzar(Duration.ofHours(12).minusSeconds(1));

        assertTrue(manager.activa());
        assertTrue(manager.actual().isPresent());
    }

    @Test
    void debeExpirarAlCumplirLasDoceHoras() {
        RelojMutable reloj = new RelojMutable(INICIO);
        SesionManager manager = new SesionManager(reloj);
        manager.iniciar(ADMIN);

        reloj.avanzar(Duration.ofHours(12));

        assertTrue(manager.actual().isEmpty(), "La sesión debe expirar a las 12 h");
        assertFalse(manager.activa());
    }

    @Test
    void debeDescartarLaSesionExpiradaDefinitivamente() {
        RelojMutable reloj = new RelojMutable(INICIO);
        SesionManager manager = new SesionManager(reloj);
        manager.iniciar(ADMIN);
        reloj.avanzar(Duration.ofHours(13));

        assertTrue(manager.actual().isEmpty());
        reloj.avanzar(Duration.ofHours(-2));
        assertFalse(manager.activa(), "Una sesión descartada no debe revivir al mover el reloj");
    }

    @Test
    void debeCerrarLaSesion() {
        SesionManager manager = new SesionManager(new RelojMutable(INICIO));
        manager.iniciar(ADMIN);

        manager.cerrar();

        assertFalse(manager.activa());
        assertTrue(manager.actual().isEmpty());
    }

    @Test
    void debeReemplazarLaSesionAnteriorAlVolverAIniciar() {
        RelojMutable reloj = new RelojMutable(INICIO);
        SesionManager manager = new SesionManager(reloj);
        manager.iniciar(ADMIN);
        reloj.avanzar(Duration.ofHours(1));

        Sesion segunda = manager.iniciar(new Usuario(1L, "admin", "hash2", true));

        assertEquals(reloj.instant(), segunda.inicio());
        assertEquals("hash2", manager.actual().orElseThrow().usuario().claveHash());
    }

    /**
     * Reloj mutable para simular el paso del tiempo sin dormir el hilo.
     */
    private static final class RelojMutable extends Clock {

        private Instant instante;

        private RelojMutable(Instant instante) {
            this.instante = instante;
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
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
