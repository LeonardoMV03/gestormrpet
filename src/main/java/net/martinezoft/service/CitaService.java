package net.martinezoft.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.martinezoft.data.CitaRepository;
import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.MascotaRepository;
import net.martinezoft.data.ServicioRepository;
import net.martinezoft.domain.Cita;
import net.martinezoft.domain.CitaDetalle;
import net.martinezoft.domain.CitaServicio;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Deudor;
import net.martinezoft.domain.Mascota;
import net.martinezoft.domain.MascotaConDueno;
import net.martinezoft.domain.MetodoPago;
import net.martinezoft.domain.Servicio;

/**
 * Casos de uso de visitas: agendar, atender, cancelar, cobrar y consultar
 * agenda y deudores. Congela el precio del servicio al agendar.
 */
public final class CitaService {

    public static final String MENSAJE_MASCOTA_INVALIDA = "La mascota no existe o está desactivada";
    public static final String MENSAJE_SERVICIO_INVALIDO = "El servicio no existe o está desactivado";
    public static final String MENSAJE_SIN_SERVICIOS = "La visita debe tener al menos un servicio";
    public static final String MENSAJE_SERVICIO_DUPLICADO = "No se puede repetir un servicio en la misma visita";
    public static final String MENSAJE_FECHA = "La fecha y hora de la visita son obligatorias";
    public static final String MENSAJE_HORARIO_OCUPADO = "La mascota ya tiene una visita en ese horario";
    public static final String MENSAJE_CITA_INEXISTENTE = "La visita no existe";
    public static final String MENSAJE_YA_CAMBIADA = "La visita cambió de estado, actualiza la agenda";

    private final CitaRepository citas;
    private final MascotaRepository mascotas;
    private final ClienteRepository clientes;
    private final ServicioRepository servicios;
    private final Clock reloj;

    public CitaService(CitaRepository citas, MascotaRepository mascotas,
                       ClienteRepository clientes, ServicioRepository servicios) {
        this(citas, mascotas, clientes, servicios, Clock.systemDefaultZone());
    }

    public CitaService(CitaRepository citas, MascotaRepository mascotas,
                       ClienteRepository clientes, ServicioRepository servicios, Clock reloj) {
        this.citas = Objects.requireNonNull(citas, "El repositorio de visitas es obligatorio");
        this.mascotas = Objects.requireNonNull(mascotas, "El repositorio de mascotas es obligatorio");
        this.clientes = Objects.requireNonNull(clientes, "El repositorio de clientes es obligatorio");
        this.servicios = Objects.requireNonNull(servicios, "El repositorio de servicios es obligatorio");
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio");
    }

    public List<MascotaConDueno> listarMascotasDisponibles() throws SQLException {
        return mascotas.listarActivasConDueno();
    }

    public List<Servicio> listarServiciosDisponibles() throws SQLException {
        return servicios.listar();
    }

    public List<CitaDetalle> listarHoy(LocalDate dia) throws SQLException {
        Objects.requireNonNull(dia, "El día es obligatorio");
        return citas.listarEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    public List<CitaDetalle> listarProximas(LocalDate desde) throws SQLException {
        Objects.requireNonNull(desde, "La fecha es obligatoria");
        return citas.listarDesde(desde.plusDays(1).atStartOfDay());
    }

    public List<CitaDetalle> listarCobrosPendientes() throws SQLException {
        return citas.listarPendientesDePago();
    }

    public List<Deudor> listarDeudores() throws SQLException {
        List<Deudor> deudores = new ArrayList<>();
        for (CitaDetalle detalle : citas.listarPendientesDePago()) {
            int indice = indiceDe(deudores, detalle.clienteId());
            Deudor actual = indice < 0 ? null : deudores.get(indice);
            Deudor agrupado = actual == null
                    ? new Deudor(detalle.clienteId(), detalle.clienteNombre(),
                            detalle.totalCentavos(), 1)
                    : new Deudor(actual.clienteId(), actual.clienteNombre(),
                            actual.totalCentavos() + detalle.totalCentavos(),
                            actual.visitas() + 1);
            if (indice < 0) {
                deudores.add(agrupado);
            } else {
                deudores.set(indice, agrupado);
            }
        }
        deudores.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.clienteNombre(), b.clienteNombre()));
        return deudores;
    }

    public CitaDetalle agendar(Long mascotaId, List<Long> servicioIds,
                               LocalDateTime fechaHora, String notas) throws SQLException {
        if (mascotaId == null) {
            throw new IllegalArgumentException(MENSAJE_MASCOTA_INVALIDA);
        }
        if (fechaHora == null) {
            throw new IllegalArgumentException(MENSAJE_FECHA);
        }
        if (servicioIds == null || servicioIds.isEmpty()) {
            throw new IllegalArgumentException(MENSAJE_SIN_SERVICIOS);
        }
        Set<Long> unicos = new HashSet<>(servicioIds);
        if (unicos.size() != servicioIds.size() || unicos.contains(null)) {
            throw new IllegalArgumentException(MENSAJE_SERVICIO_DUPLICADO);
        }

        Mascota mascota = mascotas.buscarPorId(mascotaId)
                .filter(Mascota::activo)
                .orElseThrow(() -> new IllegalArgumentException(MENSAJE_MASCOTA_INVALIDA));
        Cliente cliente = clientes.buscarPorId(mascota.clienteId())
                .filter(Cliente::activo)
                .orElseThrow(() -> new IllegalArgumentException(MENSAJE_MASCOTA_INVALIDA));

        if (citas.existeOtraCitaNoCancelada(mascotaId, fechaHora, null)) {
            throw new IllegalArgumentException(MENSAJE_HORARIO_OCUPADO);
        }

        List<CitaServicio> congelados = new ArrayList<>();
        for (Long servicioId : servicioIds) {
            Servicio servicio = servicios.buscarPorId(servicioId)
                    .filter(Servicio::activo)
                    .orElseThrow(() -> new IllegalArgumentException(MENSAJE_SERVICIO_INVALIDO));
            congelados.add(CitaServicio.de(servicio));
        }

        Cita nueva = Cita.nueva(mascotaId, fechaHora, notas);
        CitaDetalle detalle = new CitaDetalle(nueva, cliente.id(), mascota.nombre(),
                cliente.nombre(), congelados);
        return citas.insertar(detalle.cita(), detalle.servicios());
    }

    public CitaDetalle atender(long citaId) throws SQLException {
        CitaDetalle detalle = buscar(citaId);
        Cita actualizada = detalle.cita().atender();
        if (!citas.actualizarEstado(citaId, actualizada.estado())) {
            throw new IllegalArgumentException(MENSAJE_YA_CAMBIADA);
        }
        return detalle.conCita(actualizada);
    }

    public CitaDetalle cancelar(long citaId) throws SQLException {
        CitaDetalle detalle = buscar(citaId);
        Cita actualizada = detalle.cita().cancelar();
        if (!citas.actualizarEstado(citaId, actualizada.estado())) {
            throw new IllegalArgumentException(MENSAJE_YA_CAMBIADA);
        }
        return detalle.conCita(actualizada);
    }

    public CitaDetalle registrarPago(long citaId, MetodoPago metodo) throws SQLException {
        CitaDetalle detalle = buscar(citaId);
        Cita actualizada = detalle.cita().registrarPago(metodo, Instant.now(reloj));
        if (!citas.registrarPago(citaId, metodo, actualizada.pagadoEn())) {
            throw new IllegalArgumentException(MENSAJE_YA_CAMBIADA);
        }
        return detalle.conCita(actualizada);
    }

    public CitaDetalle buscar(long citaId) throws SQLException {
        return citas.buscarPorId(citaId)
                .orElseThrow(() -> new IllegalArgumentException(MENSAJE_CITA_INEXISTENTE));
    }

    private static int indiceDe(List<Deudor> deudores, Long clienteId) {
        for (int i = 0; i < deudores.size(); i++) {
            if (deudores.get(i).clienteId().equals(clienteId)) {
                return i;
            }
        }
        return -1;
    }
}
