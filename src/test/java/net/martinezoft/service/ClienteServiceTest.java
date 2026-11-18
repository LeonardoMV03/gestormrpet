package net.martinezoft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.data.ClienteRepository;
import net.martinezoft.data.Database;
import net.martinezoft.data.DatabaseInitializer;
import net.martinezoft.domain.Cliente;

class ClienteServiceTest {

    @TempDir
    Path tempDir;

    private ClienteService servicio;

    @BeforeEach
    void prepararServicio() {
        Database database = new Database(tempDir.resolve("clientes-servicio.db"));
        new DatabaseInitializer(database).migrar();
        servicio = new ClienteService(new ClienteRepository(database));
    }

    @Test
    void debeCrearYListarClientes() throws SQLException {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", "555", null, null, null));

        assertEquals(List.of(creado), servicio.listar());
    }

    @Test
    void debeRechazarClienteNulo() {
        assertThrows(NullPointerException.class, () -> servicio.crear(null));
        assertThrows(NullPointerException.class, () -> servicio.actualizar(null));
    }

    @Test
    void debeRechazarActualizarSinId() {
        Cliente sinId = Cliente.nuevo("Ana", null, null, null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(sinId));
        assertEquals("No se puede actualizar un cliente sin id", error.getMessage());
    }

    @Test
    void debeRechazarActualizarUnClienteInexistente() {
        Cliente inexistente = new Cliente(999L, "Ana", null, null, null, null, true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(inexistente));
        assertEquals("El cliente no existe o está desactivado", error.getMessage());
    }

    @Test
    void debeActualizarYDesactivar() throws SQLException {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        Cliente editado = new Cliente(creado.id(), "Ana Pérez", "555", null, null, null, true);

        assertEquals(editado, servicio.actualizar(editado));

        servicio.desactivar(creado.id());

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void debeRechazarDesactivarDosVeces() throws SQLException {
        Cliente creado = servicio.crear(Cliente.nuevo("Ana", null, null, null, null));
        servicio.desactivar(creado.id());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.desactivar(creado.id()));
        assertEquals("El cliente no existe o ya está desactivado", error.getMessage());
    }

    @Test
    void debeBuscarPorNombreOTelefono() throws SQLException {
        servicio.crear(Cliente.nuevo("Ana Pérez", "555-1234", null, null, null));
        servicio.crear(Cliente.nuevo("Bruno", "555-9999", null, null, null));

        assertEquals(1, servicio.buscar("ana").size());
        assertEquals(1, servicio.buscar("1234").size());
        assertEquals(2, servicio.buscar("").size());
    }
}
