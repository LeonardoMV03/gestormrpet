package net.martinezoft.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.domain.Usuario;

/**
 * Acceso JDBC a la tabla {@code usuarios}. La aplicación maneja un único
 * usuario, pero el repositorio no impone esa regla.
 */
public final class UsuarioRepository {

    private static final String COLUMNAS = "id, usuario, clave_hash, debe_cambiar_password";

    /** Marca de tiempo UTC en ISO-8601, coherente con los defaults del esquema. */
    private static final String AHORA_ISO = "strftime('%Y-%m-%dT%H:%M:%SZ', 'now')";

    private final Database database;

    public UsuarioRepository(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
    }

    public Optional<Usuario> buscarPorUsuario(String usuario) throws SQLException {
        Objects.requireNonNull(usuario, "El usuario es obligatorio");
        String sql = "SELECT " + COLUMNAS + " FROM usuarios WHERE usuario = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    public Optional<Usuario> buscarPorId(long id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM usuarios WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    public Usuario insertar(String usuario, String claveHash, boolean debeCambiarPassword) throws SQLException {
        Objects.requireNonNull(usuario, "El usuario es obligatorio");
        Objects.requireNonNull(claveHash, "El hash de la contraseña es obligatorio");
        String sql = """
                INSERT INTO usuarios (usuario, clave_hash, debe_cambiar_password)
                VALUES (?, ?, ?)
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, usuario);
            sentencia.setString(2, claveHash);
            sentencia.setInt(3, debeCambiarPassword ? 1 : 0);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("SQLite no devolvió el id del usuario insertado");
                }
                return new Usuario(claves.getLong(1), usuario, claveHash, debeCambiarPassword);
            }
        }
    }

    /**
     * Actualiza el hash y el aviso de cambio de contraseña.
     *
     * @return {@code true} si se actualizó alguna fila.
     */
    public boolean actualizarClave(long id, String claveHash, boolean debeCambiarPassword) throws SQLException {
        Objects.requireNonNull(claveHash, "El hash de la contraseña es obligatorio");
        String sql = """
                UPDATE usuarios
                   SET clave_hash = ?, debe_cambiar_password = ?, actualizado_en = %s
                 WHERE id = ?
                """.formatted(AHORA_ISO);
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, claveHash);
            sentencia.setInt(2, debeCambiarPassword ? 1 : 0);
            sentencia.setLong(3, id);
            return sentencia.executeUpdate() > 0;
        }
    }

    public long contar() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            return filas.next() ? filas.getLong(1) : 0;
        }
    }

    private static Usuario mapear(ResultSet filas) throws SQLException {
        return new Usuario(
                filas.getLong("id"),
                filas.getString("usuario"),
                filas.getString("clave_hash"),
                filas.getInt("debe_cambiar_password") == 1);
    }
}
