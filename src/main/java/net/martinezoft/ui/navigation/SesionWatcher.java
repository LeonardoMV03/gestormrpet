package net.martinezoft.ui.navigation;

import java.util.Objects;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Comprueba periódicamente que la sesión sigue vigente. La expiración se detecta
 * con un retraso de hasta un periodo, aceptable para una sesión de 12 horas.
 */
public final class SesionWatcher {

    static final Duration PERIODO = Duration.seconds(60);

    private final ShellViewModel shell;
    private final Runnable alExpirar;
    private final Timeline temporizador;

    public SesionWatcher(ShellViewModel shell, Runnable alExpirar) {
        this.shell = Objects.requireNonNull(shell, "El ViewModel del shell es obligatorio");
        this.alExpirar = Objects.requireNonNull(alExpirar, "El callback de expiración es obligatorio");
        this.temporizador = new Timeline(new KeyFrame(PERIODO, evento -> comprobar()));
        this.temporizador.setCycleCount(Animation.INDEFINITE);
    }

    public void iniciar() {
        temporizador.play();
    }

    public void detener() {
        temporizador.stop();
    }

    private void comprobar() {
        if (!shell.sesionVigente()) {
            detener();
            alExpirar.run();
        }
    }
}
