package net.martinezoft.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.domain.Cita;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.CitaServicio;
import net.martinezoft.domain.EstadoCita;
import net.martinezoft.domain.EstadoPago;
import net.martinezoft.domain.MetodoPago;

/**
 * Acceso JDBC a {@code citas} y {@code cita_servicios}. Crear una visita es una
 * transacción: o se guardan la cabecera y todos sus servicios, o nada.
 */
public final class CitaRepository {

    private static final String SELECT_CITAS = """
            SELECT c.id, c.mascota_id, c.fecha_hora, c.estado, c.notas,
                   c.estado_pago, c.metodo_pago, c.pagado_en,
                   m.nombre AS mascota_nombre, cl.id AS cliente_id, cl.nombre AS cliente_nombre
              FROM citas c
              JOIN mascotas m ON m.id = c.mascota_id
              JOIN clientes cl ON cl.id = m.cliente_id
            """;

    private static final String ORDEN = " ORDER BY c.fecha_hora, c.id";

    private final Database database;

    public CitaRepository(Database database) {
        this.database = Objects.requireNonNull(database, "La base de datos es obligatoria");
    }

    /**
     * Inserta la visita y sus servicios en una transacción. Los precios se
     * reciben ya congelados desde el servicio de aplicación.
     */
    public CitaDetalle insertar(Cita cita, List<CitaServicio> servicios) throws SQLException {
        Objects.requireNonNull(cita, "La cita es obligatoria");
        if (servicios == null || servicios.isEmpty()) {
            throw new IllegalArgumentException("La visita debe tener al menos un servicio");
        }
        String sqlCita = """
                INSERT INTO citas (mascota_id, fecha_hora, estado, notas,
                                   estado_pago, metodo_pago, pagado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String sqlServicio = """
                INSERT INTO cita_servicios (cita_id, servicio_id, precio_centavos)
                VALUES (?, ?, ?)
                """;
        long id;
        try (Connection conexion = database.conexion()) {
            conexion.setAutoCommit(false);
            try {
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        sqlCita, Statement.RETURN_GENERATED_KEYS)) {
                    sentencia.setLong(1, cita.mascotaId());
                    sentencia.setString(2, cita.fechaHora().toString());
                    sentencia.setString(3, cita.estado().valor());
                    sentencia.setString(4, cita.notas());
                    sentencia.setString(5, cita.estadoPago().valor());
                    asignarMetodo(sentencia, 6, cita.metodoPago());
                    asignarInstante(sentencia, 7, cita.pagadoEn());
                    sentencia.executeUpdate();
                    try (ResultSet claves = sentencia.getGeneratedKeys()) {
                        if (!claves.next()) {
                            throw new SQLException("SQLite no devolvió el id de la visita insertada");
                        }
                        id = claves.getLong(1);
                    }
                }
                try (PreparedStatement sentencia = conexion.prepareStatement(sqlServicio)) {
                    for (CitaServicio servicio : servicios) {
                        sentencia.setLong(1, id);
                        sentencia.setLong(2, servicio.servicioId());
                        sentencia.setLong(3, servicio.precioCentavos());
                        sentencia.addBatch();
                    }
                    sentencia.executeBatch();
                }
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
        return buscarPorId(id)
                .orElseThrow(() -> new SQLException("No se pudo releer la visita insertada"));
    }

    public Optional<CitaDetalle> buscarPorId(long id) throws SQLException {
        List<CitaDetalle> encontradas = consultar(SELECT_CITAS + " WHERE c.id = ?", List.of(id));
        return encontradas.isEmpty() ? Optional.empty() : Optional.of(encontradas.get(0));
    }

    public List<CitaDetalle> listarEntre(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        Objects.requireNonNull(desde, "El inicio del rango es obligatorio");
        Objects.requireNonNull(hasta, "El fin del rango es obligatorio");
        return consultar(SELECT_CITAS + " WHERE c.fecha_hora >= ? AND c.fecha_hora < ?" + ORDEN,
                List.of(desde.toString(), hasta.toString()));
    }

    public List<CitaDetalle> listarDesde(LocalDateTime desde) throws SQLException {
        Objects.requireNonNull(desde, "El inicio es obligatorio");
        return consultar(SELECT_CITAS + " WHERE c.fecha_hora >= ?" + ORDEN, List.of(desde.toString()));
    }

    /**
     * Visitas atendidas que siguen sin pago, de la más antigua a la más nueva.
     */
    public List<CitaDetalle> listarPendientesDePago() throws SQLException {
        return consultar(SELECT_CITAS
                + " WHERE c.estado = 'atendida' AND c.estado_pago = 'a_deber'" + ORDEN, List.of());
    }

    /**
     * Indica si la mascota ya tiene otra visita no cancelada en ese momento.
     */
    public boolean existeOtraCitaNoCancelada(long mascotaId, LocalDateTime fechaHora,
                                             Long excluirCitaId) throws SQLException {
        String sql = "SELECT 1 FROM citas WHERE mascota_id = ? AND fecha_hora = ?"
                + " AND estado <> 'cancelada'"
                + (excluirCitaId == null ? "" : " AND id <> ?");
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, mascotaId);
            sentencia.setString(2, fechaHora.toString());
            if (excluirCitaId != null) {
                sentencia.setLong(3, excluirCitaId);
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        }
    }

    public boolean actualizarEstado(long citaId, EstadoCita estado) throws SQLException {
        String sql = "UPDATE citas SET estado = ? WHERE id = ? AND estado = 'pendiente'";
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado.valor());
            sentencia.setLong(2, citaId);
            return sentencia.executeUpdate() > 0;
        }
    }

    public boolean registrarPago(long citaId, MetodoPago metodo, Instant pagadoEn) throws SQLException {
        String sql = """
                UPDATE citas
                   SET estado_pago = 'pagado', metodo_pago = ?, pagado_en = ?
                 WHERE id = ? AND estado = 'atendida' AND estado_pago = 'a_deber'
                """;
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, metodo.valor());
            sentencia.setString(2, pagadoEn.toString());
            sentencia.setLong(3, citaId);
            return sentencia.executeUpdate() > 0;
        }
    }

    private List<CitaDetalle> consultar(String sql, List<Object> parametros) throws SQLException {
        try (Connection conexion = database.conexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                sentencia.setObject(i + 1, parametros.get(i));
            }
            List<FilaCita> filas = new ArrayList<>();
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    filas.add(new FilaCita(
                            mapearCita(resultado),
                            resultado.getLong("cliente_id"),
                            resultado.getString("mascota_nombre"),
                            resultado.getString("cliente_nombre")));
                }
            }
            if (filas.isEmpty()) {
                return List.of();
            }
            List<Long> ids = filas.stream().map(fila -> fila.cita().id()).toList();
            Map<Long, List<CitaServicio>> serviciosPorCita = cargarServicios(conexion, ids);
            List<CitaDetalle> detalles = new ArrayList<>();
            for (FilaCita fila : filas) {
                List<CitaServicio> servicios = serviciosPorCita.get(fila.cita().id());
                if (servicios == null || servicios.isEmpty()) {
                    throw new SQLException("La visita " + fila.cita().id() + " no tiene servicios");
                }
                detalles.add(new CitaDetalle(fila.cita(), fila.clienteId(),
                        fila.mascotaNombre(), fila.clienteNombre(), servicios));
            }
            return detalles;
        }
    }

    private static Map<Long, List<CitaServicio>> cargarServicios(Connection conexion, List<Long> ids)
            throws SQLException {
        String marcadores = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = """
                SELECT cs.cita_id, cs.servicio_id, s.nombre AS servicio_nombre, cs.precio_centavos
                  FROM cita_servicios cs
                  JOIN servicios s ON s.id = cs.servicio_id
                 WHERE cs.cita_id IN (%s)
                 ORDER BY s.nombre COLLATE NOCASE
                """.formatted(marcadores);
        Map<Long, List<CitaServicio>> servicios = new HashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                sentencia.setLong(i + 1, ids.get(i));
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    long citaId = filas.getLong("cita_id");
                    servicios.computeIfAbsent(citaId, clave -> new ArrayList<>())
                            .add(new CitaServicio(citaId, filas.getLong("servicio_id"),
                                    filas.getString("servicio_nombre"),
                                    filas.getLong("precio_centavos")));
                }
            }
        }
        return servicios;
    }

    private static Cita mapearCita(ResultSet filas) throws SQLException {
        String metodo = filas.getString("metodo_pago");
        return new Cita(
                filas.getLong("id"),
                filas.getLong("mascota_id"),
                parsearFechaHora(filas.getString("fecha_hora")),
                EstadoCita.desde(filas.getString("estado")),
                filas.getString("notas"),
                EstadoPago.desde(filas.getString("estado_pago")),
                metodo == null ? null : MetodoPago.desde(metodo),
                parsearInstante(filas.getString("pagado_en")));
    }

    private static LocalDateTime parsearFechaHora(String texto) throws SQLException {
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(texto).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw new SQLException("Fecha de visita inválida: " + texto, ex);
            }
        }
    }

    private static Instant parsearInstante(String texto) throws SQLException {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(texto).atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException ex) {
                throw new SQLException("Fecha de pago inválida: " + texto, ex);
            }
        }
    }

    private static void asignarMetodo(PreparedStatement sentencia, int indice, MetodoPago metodo)
            throws SQLException {
        if (metodo == null) {
            sentencia.setNull(indice, Types.VARCHAR);
        } else {
            sentencia.setString(indice, metodo.valor());
        }
    }

    private static void asignarInstante(PreparedStatement sentencia, int indice, Instant instante)
            throws SQLException {
        if (instante == null) {
            sentencia.setNull(indice, Types.VARCHAR);
        } else {
            sentencia.setString(indice, instante.toString());
        }
    }

    private record FilaCita(Cita cita, long clienteId, String mascotaNombre, String clienteNombre) {
    }
}
