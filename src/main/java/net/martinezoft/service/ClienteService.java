package net.martinezoft.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.martinezoft.data.ClienteRepository;
import net.martinezoft.domain.Cliente;

/**
 * Casos de uso de clientes: alta, edición, baja lógica y búsqueda. Las reglas
 * de los campos viven en {@link Cliente}.
 */
public final class ClienteService {

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio,
                "El repositorio de clientes es obligatorio");
    }

    public List<Cliente> listar() throws SQLException {
        return repositorio.listar();
    }

    public List<Cliente> buscar(String texto) throws SQLException {
        return repositorio.buscar(texto);
    }

    public Optional<Cliente> buscarPorId(long id) throws SQLException {
        return repositorio.buscarPorId(id);
    }

    public Cliente crear(Cliente cliente) throws SQLException {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        return repositorio.insertar(cliente);
    }

    public Cliente actualizar(Cliente cliente) throws SQLException {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        if (cliente.id() == null) {
            throw new IllegalArgumentException("No se puede actualizar un cliente sin id");
        }
        if (!repositorio.actualizar(cliente)) {
            throw new IllegalArgumentException("El cliente no existe o está desactivado");
        }
        return cliente;
    }

    public void desactivar(long id) throws SQLException {
        if (!repositorio.desactivar(id)) {
            throw new IllegalArgumentException("El cliente no existe o ya está desactivado");
        }
    }
}
