package net.martinezoft.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.config.SesionManager;
import net.martinezoft.config.SesionManager.Sesion;
import net.martinezoft.data.UsuarioRepository;
import net.martinezoft.domain.Usuario;
import net.martinezoft.security.PasswordHasher;

/**
 * Autenticación del usuario único {@code admin}. Nunca registra contraseñas ni
 * hashes en logs.
 */
public final class AuthService {

    public static final String USUARIO_ADMIN = "admin";
    public static final String CLAVE_INICIAL = "admin";
    public static final int LONGITUD_MINIMA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordHasher hasher;
    private final SesionManager sesion;

    public AuthService(UsuarioRepository usuarios, PasswordHasher hasher, SesionManager sesion) {
        this.usuarios = Objects.requireNonNull(usuarios, "El repositorio de usuarios es obligatorio");
        this.hasher = Objects.requireNonNull(hasher, "El hasher es obligatorio");
        this.sesion = Objects.requireNonNull(sesion, "El gestor de sesión es obligatorio");
    }

    /**
     * Crea el usuario {@code admin} con contraseña inicial {@code admin} solo si
     * la tabla está vacía. Es idempotente.
     */
    public void asegurarAdmin() throws SQLException {
        if (usuarios.contar() > 0) {
            return;
        }
        usuarios.insertar(USUARIO_ADMIN, hasher.hash(CLAVE_INICIAL), true);
    }

    /**
     * Valida credenciales y, si son correctas, inicia la sesión.
     */
    public Optional<Sesion> iniciarSesion(String usuario, String password) throws SQLException {
        if (usuario == null || usuario.isBlank() || password == null) {
            return Optional.empty();
        }
        Optional<Usuario> encontrado = usuarios.buscarPorUsuario(usuario);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuarioEncontrado = encontrado.get();
        if (!hasher.verificar(password, usuarioEncontrado.claveHash())) {
            return Optional.empty();
        }
        return Optional.of(sesion.iniciar(usuarioEncontrado));
    }

    /**
     * Cambia la contraseña validando la actual. La nueva debe tener al menos
     * {@value #LONGITUD_MINIMA} caracteres y ser distinta de la actual.
     */
    public void cambiarClave(long usuarioId, String actual, String nueva) throws SQLException {
        if (actual == null || nueva == null) {
            throw new IllegalArgumentException("Debe indicar la contraseña actual y la nueva");
        }
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (!hasher.verificar(actual, usuario.claveHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (nueva.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (hasher.verificar(nueva, usuario.claveHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta a la actual");
        }
        boolean actualizado = usuarios.actualizarClave(usuarioId, hasher.hash(nueva), false);
        if (!actualizado) {
            throw new IllegalStateException("No se pudo actualizar la contraseña del usuario " + usuarioId);
        }
    }
}
