package net.martinezoft.service;

import java.sql.SQLException;
import java.text.Normalizer;
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
        asegurarNombreUnico(servicio.nombre(), null);
        return repositorio.insertar(servicio);
    }

    public Servicio actualizar(Servicio servicio) throws SQLException {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        if (servicio.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar un servicio sin id");
        }
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
        String normalizado = normalizar(nombre);
        boolean duplicado = repositorio.listarTodos().stream()
                .filter(servicio -> !Objects.equals(servicio.id(), idPropio))
                .anyMatch(servicio -> normalizar(servicio.nombre()).equalsIgnoreCase(normalizado));
        if (duplicado) {
            throw new IllegalArgumentException(MENSAJE_NOMBRE_DUPLICADO);
        }
    }

    private static String normalizar(String nombre) {
        return Normalizer.normalize(nombre.trim(), Normalizer.Form.NFC);
    }
}
