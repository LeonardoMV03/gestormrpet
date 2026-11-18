package net.martinezoft.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.MascotaRepository;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Mascota;

/**
 * Casos de uso de mascotas. Toda mascota debe pertenecer a un cliente activo.
 * Las reglas de los campos viven en {@link Mascota}.
 */
public final class MascotaService {

    private static final String MENSAJE_CLIENTE_INVALIDO = "El cliente no existe o está desactivado";

    private final MascotaRepository repositorio;
    private final ClienteRepository clientes;

    public MascotaService(MascotaRepository repositorio, ClienteRepository clientes) {
        this.repositorio = Objects.requireNonNull(repositorio,
                "El repositorio de mascotas es obligatorio");
        this.clientes = Objects.requireNonNull(clientes, "El repositorio de clientes es obligatorio");
    }

    public List<Mascota> listarPorCliente(long clienteId) throws SQLException {
        return repositorio.listarPorCliente(clienteId);
    }

    public Optional<Mascota> buscarPorId(long id) throws SQLException {
        return repositorio.buscarPorId(id);
    }

    public Mascota crear(Mascota mascota) throws SQLException {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        asegurarClienteActivo(mascota.clienteId());
        return repositorio.insertar(mascota);
    }

    public Mascota actualizar(Mascota mascota) throws SQLException {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        if (mascota.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar una mascota sin id");
        }
        asegurarClienteActivo(mascota.clienteId());
        if (!repositorio.actualizar(mascota)) {
            throw new IllegalArgumentException("La mascota no existe o está desactivada");
        }
        return mascota;
    }

    public void desactivar(long id) throws SQLException {
        if (!repositorio.desactivar(id)) {
            throw new IllegalArgumentException("La mascota no existe o ya está desactivada");
        }
    }

    private void asegurarClienteActivo(Long clienteId) throws SQLException {
        Optional<Cliente> cliente = clientes.buscarPorId(clienteId);
        if (cliente.isEmpty() || !cliente.get().activo()) {
            throw new IllegalArgumentException(MENSAJE_CLIENTE_INVALIDO);
        }
    }
}
