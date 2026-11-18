package net.martinezoft.domain;

/**
 * Usuario único de la aplicación (por ahora solo {@code admin}). No se modelan
 * roles ni múltiples cuentas.
 */
public record Usuario(Long id, String usuario, String claveHash, boolean debeCambiarPassword) {

    /** Omite {@code claveHash} para que el hash nunca salga por logs o depuración. */
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", usuario=" + usuario + ", debeCambiarPassword=" + debeCambiarPassword + "]";
    }
}
