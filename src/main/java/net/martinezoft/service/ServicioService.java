package net.martinezoft.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.data.ServicioRepository;
import net.martinezoft.domain.Servicio;

/**
 * Casos de uso de servicios: alta, edición, baja lógica y consultas. Valida los
 * campos y garantiza que el nombre no se repita, incluso entre desactivados.
 */
public final class ServicioService {

    public static final String MENSAJE_NOMBRE_DUPLICADO = "Ya existe un servicio con ese nombre";

    private final ServicioRepository repositorio;

    public ServicioService(ServicioRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio,
                "El repositorio de servicios es obligatorio");
    }

    public List<Servicio> listar() throws SQLException {
        return repositorio.listar();
    }

    public Optional<Servicio> buscarPorId(long id) throws SQLException {
        return repositorio.buscarPorId(id);
    }

    public Servicio crear(Servicio servicio) throws SQLException {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        validar(servicio);
        asegurarNombreUnico(servicio.nombre(), null);
        return repositorio.insertar(servicio);
    }

    public Servicio actualizar(Servicio servicio) throws SQLException {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        if (servicio.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar un servicio sin id");
        }
        validar(servicio);
        asegurarNombreUnico(servicio.nombre(), servicio.id());
        if (!repositorio.actualizar(servicio)) {
            throw new IllegalArgumentException("El servicio no existe o está desactivado");
        }
        return servicio;
    }

    public void desactivar(long id) throws SQLException {
        if (!repositorio.desactivar(id)) {
            throw new IllegalArgumentException("El servicio no existe o ya está desactivado");
        }
    }

    private void asegurarNombreUnico(String nombre, Long idPropio) throws SQLException {
        Optional<Servicio> existente = repositorio.buscarPorNombre(nombre);
        if (existente.isPresent() && !existente.get().id().equals(idPropio)) {
            throw new IllegalArgumentException(MENSAJE_NOMBRE_DUPLICADO);
        }
    }

    private static void validar(Servicio servicio) {
        if (servicio.nombre() == null || servicio.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        if (servicio.precioCentavos() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (servicio.duracionMin() != null && servicio.duracionMin() < 0) {
            throw new IllegalArgumentException("La duración no puede ser negativa");
        }
    }
}
