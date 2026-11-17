package net.martinezoft.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.domain.Cliente;

/**
 * Acceso JDBC a {@code clientes}. El borrado es lógico: {@link #desactivar(long)}
 * marca {@code activo=0} y {@link #listar()} solo devuelve activos.
 */
public final class ClienteRepository {

    private static final String COLUMNAS = "id, nombre, telefono, email, direccion, notas, activo";

    /** Marca de tiempo UTC en ISO-8601, coherente con los defaults del esquema. */
    private static final String AHORA_ISO = "strftime('%Y-%m-%dT%H:%M:%SZ', 'now')";

    private final Database database;

    public ClienteRepository(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
    }

    public Cliente insertar(Cliente cliente) throws SQLException {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        String sql = """
                INSERT INTO clientes (nombre, telefono, email, direccion, notas, activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, cliente.nombre());
            sentencia.setString(2, cliente.telefono());
            sentencia.setString(3, cliente.email());
            sentencia.setString(4, cliente.direccion());
            sentencia.setString(5, cliente.notas());
            sentencia.setInt(6, cliente.activo() ? 1 : 0);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("SQLite no devolvió el id del cliente insertado");
                }
                return conId(cliente, claves.getLong(1));
            }
        }
    }

    /**
     * Busca por id incluyendo clientes inactivos, para poder consultar el estado
     * tras un borrado lógico.
     */
    public Optional<Cliente> buscarPorId(long id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM clientes WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    public List<Cliente> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM clientes WHERE activo = 1 ORDER BY nombre COLLATE NOCASE";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                clientes.add(mapear(filas));
            }
        }
        return clientes;
    }

    /**
     * Actualiza los datos editables de un cliente activo.
     *
     * @return {@code true} si se actualizó alguna fila.
     */
    public boolean actualizar(Cliente cliente) throws SQLException {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        if (cliente.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar un cliente sin id");
        }
        String sql = """
                UPDATE clientes
                   SET nombre = ?, telefono = ?, email = ?, direccion = ?, notas = ?,
                       actualizado_en = %s
                 WHERE id = ? AND activo = 1
                """.formatted(AHORA_ISO);
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, cliente.nombre());
            sentencia.setString(2, cliente.telefono());
            sentencia.setString(3, cliente.email());
            sentencia.setString(4, cliente.direccion());
            sentencia.setString(5, cliente.notas());
            sentencia.setLong(6, cliente.id());
            return sentencia.executeUpdate() > 0;
        }
    }

    /**
     * Borrado lógico de un cliente activo.
     *
     * @return {@code true} si se desactivó alguna fila.
     */
    public boolean desactivar(long id) throws SQLException {
        String sql = "UPDATE clientes SET activo = 0, actualizado_en = " + AHORA_ISO
                + " WHERE id = ? AND activo = 1";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            return sentencia.executeUpdate() > 0;
        }
    }

    private static Cliente mapear(ResultSet filas) throws SQLException {
        return new Cliente(
                filas.getLong("id"),
                filas.getString("nombre"),
                filas.getString("telefono"),
                filas.getString("email"),
                filas.getString("direccion"),
                filas.getString("notas"),
                filas.getInt("activo") == 1);
    }

    private static Cliente conId(Cliente cliente, long id) {
        return new Cliente(id, cliente.nombre(), cliente.telefono(), cliente.email(),
                cliente.direccion(), cliente.notas(), cliente.activo());
    }
}
