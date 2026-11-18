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

import net.martinezoft.domain.Mascota;

/**
 * Acceso JDBC a {@code mascotas}. El borrado es lógico: {@link #desactivar(long)}
 * marca {@code activo=0} y {@link #listarPorCliente(long)} solo devuelve activas.
 */
public final class MascotaRepository {

    private static final String COLUMNAS =
            "id, cliente_id, nombre, especie, raza, sexo, fecha_nacimiento, notas, activo";

    private static final String AHORA_ISO = "strftime('%Y-%m-%dT%H:%M:%SZ', 'now')";

    private final Database database;

    public MascotaRepository(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
    }

    public Mascota insertar(Mascota mascota) throws SQLException {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        String sql = """
                INSERT INTO mascotas (cliente_id, nombre, especie, raza, sexo, fecha_nacimiento, notas, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setLong(1, mascota.clienteId());
            sentencia.setString(2, mascota.nombre());
            sentencia.setString(3, mascota.especie());
            sentencia.setString(4, mascota.raza());
            sentencia.setString(5, mascota.sexo());
            sentencia.setString(6, mascota.fechaNacimiento());
            sentencia.setString(7, mascota.notas());
            sentencia.setInt(8, mascota.activo() ? 1 : 0);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("SQLite no devolvió el id de la mascota insertada");
                }
                return conId(mascota, claves.getLong(1));
            }
        }
    }

    public Optional<Mascota> buscarPorId(long id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM mascotas WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    public List<Mascota> listarPorCliente(long clienteId) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM mascotas WHERE cliente_id = ? AND activo = 1"
                + " ORDER BY nombre COLLATE NOCASE";
        List<Mascota> mascotas = new ArrayList<>();
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, clienteId);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    mascotas.add(mapear(filas));
                }
            }
        }
        return mascotas;
    }

    public boolean actualizar(Mascota mascota) throws SQLException {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        if (mascota.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar una mascota sin id");
        }
        String sql = """
                UPDATE mascotas
                   SET nombre = ?, especie = ?, raza = ?, sexo = ?, fecha_nacimiento = ?, notas = ?,
                       actualizado_en = %s
                 WHERE id = ? AND activo = 1
                """.formatted(AHORA_ISO);
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, mascota.nombre());
            sentencia.setString(2, mascota.especie());
            sentencia.setString(3, mascota.raza());
            sentencia.setString(4, mascota.sexo());
            sentencia.setString(5, mascota.fechaNacimiento());
            sentencia.setString(6, mascota.notas());
            sentencia.setLong(7, mascota.id());
            return sentencia.executeUpdate() > 0;
        }
    }

    public boolean desactivar(long id) throws SQLException {
        String sql = "UPDATE mascotas SET activo = 0, actualizado_en = " + AHORA_ISO
                + " WHERE id = ? AND activo = 1";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            return sentencia.executeUpdate() > 0;
        }
    }

    private static Mascota mapear(ResultSet filas) throws SQLException {
        return new Mascota(
                filas.getLong("id"),
                filas.getLong("cliente_id"),
                filas.getString("nombre"),
                filas.getString("especie"),
                filas.getString("raza"),
                filas.getString("sexo"),
                filas.getString("fecha_nacimiento"),
                filas.getString("notas"),
                filas.getInt("activo") == 1);
    }

    private static Mascota conId(Mascota mascota, long id) {
        return new Mascota(id, mascota.clienteId(), mascota.nombre(), mascota.especie(),
                mascota.raza(), mascota.sexo(), mascota.fechaNacimiento(), mascota.notas(),
                mascota.activo());
    }
}
