package net.martinezoft.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.domain.Servicio;

/**
 * Acceso JDBC a {@code servicios}. El borrado es lógico: {@link #desactivar(long)}
 * marca {@code activo=0} y {@link #listar()} solo devuelve activos.
 */
public final class ServicioRepository {

    private static final String COLUMNAS =
            "id, nombre, descripcion, precio_centavos, duracion_min, activo";

    private final Database database;

    public ServicioRepository(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
    }

    public Servicio insertar(Servicio servicio) throws SQLException {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        String sql = """
                INSERT INTO servicios (nombre, descripcion, precio_centavos, duracion_min, activo)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, servicio.nombre());
            sentencia.setString(2, servicio.descripcion());
            sentencia.setLong(3, servicio.precioCentavos());
            asignarDuracion(sentencia, 4, servicio.duracionMin());
            sentencia.setInt(5, servicio.activo() ? 1 : 0);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("SQLite no devolvió el id del servicio insertado");
                }
                return conId(servicio, claves.getLong(1));
            }
        }
    }

    /**
     * Busca por id incluyendo servicios inactivos, para poder consultar el
     * estado tras un borrado lógico.
     */
    public Optional<Servicio> buscarPorId(long id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM servicios WHERE id = ?";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    /**
     * Consulta exacta por nombre incluyendo inactivos. El {@code COLLATE NOCASE}
     * de SQLite solo pliega mayúsculas ASCII, por lo que no sirve para comparar
     * nombres Unicode; la unicidad real la garantiza {@code ServicioService}.
     */
    public Optional<Servicio> buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM servicios WHERE nombre = ? COLLATE NOCASE";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombre);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        }
    }

    public List<Servicio> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM servicios WHERE activo = 1 ORDER BY nombre COLLATE NOCASE";
        List<Servicio> servicios = new ArrayList<>();
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                servicios.add(mapear(filas));
            }
        }
        return servicios;
    }

    /**
     * Lista todas las filas, activas e inactivas, ordenadas por nombre. Es la
     * base para validar la unicidad sin depender del plegado ASCII de SQLite.
     */
    public List<Servicio> listarTodos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM servicios ORDER BY nombre COLLATE NOCASE";
        List<Servicio> servicios = new ArrayList<>();
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                servicios.add(mapear(filas));
            }
        }
        return servicios;
    }

    /**
     * Actualiza los datos editables de un servicio activo.
     *
     * @return {@code true} si se actualizó alguna fila.
     */
    public boolean actualizar(Servicio servicio) throws SQLException {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        if (servicio.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar un servicio sin id");
        }
        String sql = """
                UPDATE servicios
                   SET nombre = ?, descripcion = ?, precio_centavos = ?, duracion_min = ?
                 WHERE id = ? AND activo = 1
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, servicio.nombre());
            sentencia.setString(2, servicio.descripcion());
            sentencia.setLong(3, servicio.precioCentavos());
            asignarDuracion(sentencia, 4, servicio.duracionMin());
            sentencia.setLong(5, servicio.id());
            return sentencia.executeUpdate() > 0;
        }
    }

    /**
     * Borrado lógico de un servicio activo.
     *
     * @return {@code true} si se desactivó alguna fila.
     */
    public boolean desactivar(long id) throws SQLException {
        String sql = "UPDATE servicios SET activo = 0 WHERE id = ? AND activo = 1";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            return sentencia.executeUpdate() > 0;
        }
    }

    private static void asignarDuracion(PreparedStatement sentencia, int indice, Integer duracion)
            throws SQLException {
        if (duracion == null) {
            sentencia.setNull(indice, Types.INTEGER);
        } else {
            sentencia.setInt(indice, duracion);
        }
    }

    private static Servicio mapear(ResultSet filas) throws SQLException {
        Integer duracion = null;
        Object valorDuracion = filas.getObject("duracion_min");
        if (valorDuracion != null) {
            duracion = filas.getInt("duracion_min");
            if (filas.wasNull()) {
                duracion = null;
            }
        }
        return new Servicio(
                filas.getLong("id"),
                filas.getString("nombre"),
                filas.getString("descripcion"),
                filas.getLong("precio_centavos"),
                duracion,
                filas.getInt("activo") == 1);
    }

    private static Servicio conId(Servicio servicio, long id) {
        return new Servicio(id, servicio.nombre(), servicio.descripcion(),
                servicio.precioCentavos(), servicio.duracionMin(), servicio.activo());
    }
}
