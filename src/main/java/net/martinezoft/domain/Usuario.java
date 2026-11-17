package net.martinezoft.domain;

/**
 * Usuario único de la aplicación (por ahora solo {@code admin}). No se modelan
 * roles ni múltiples cuentas.
 */
public record Usuario(Long id, String usuario, String claveHash, boolean debeCambiarPassword) {
}
