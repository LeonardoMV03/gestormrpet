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
import net.martinezoft.data.MascotaRepository;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Mascota;

class MascotaServiceTest {

    @TempDir
    Path tempDir;

    private ClienteRepository clientes;
    private MascotaService servicio;
    private Long clienteId;

    @BeforeEach
    void prepararServicio() throws SQLException {
        Database database = new Database(tempDir.resolve("mascotas-servicio.db"));
        new DatabaseInitializer(database).migrar();
        clientes = new ClienteRepository(database);
        servicio = new MascotaService(new MascotaRepository(database), clientes);
        clienteId = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null)).id();
    }

    @Test
    void debeCrearYListarMascotasDelCliente() throws SQLException {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));

        assertEquals(List.of(creada), servicio.listarPorCliente(clienteId));
    }

    @Test
    void debeRechazarMascotaNula() {
        assertThrows(NullPointerException.class, () -> servicio.crear(null));
        assertThrows(NullPointerException.class, () -> servicio.actualizar(null));
    }

    @Test
    void debeRechazarMascotaDeUnClienteInexistente() {
        Mascota huerfana = Mascota.nueva(999L, "Firulais", null, null, null, null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(huerfana));
        assertEquals("El cliente no existe o está desactivado", error.getMessage());
    }

    @Test
    void debeRechazarMascotaDeUnClienteDesactivado() throws SQLException {
        clientes.desactivar(clienteId);
        Mascota mascota = Mascota.nueva(clienteId, "Firulais", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(mascota));
    }

    @Test
    void debeRechazarActualizarSinId() {
        Mascota sinId = Mascota.nueva(clienteId, "Firulais", null, null, null, null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizar(sinId));
        assertEquals("No se puede actualizar una mascota sin id", error.getMessage());
    }

    @Test
    void debeActualizarYDesactivar() throws SQLException {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        Mascota editada = new Mascota(creada.id(), clienteId, "Firulais", "perro", "Beagle",
                "macho", null, null, true);

        assertEquals(editada, servicio.actualizar(editada));
        servicio.desactivar(creada.id());

        assertTrue(servicio.listarPorCliente(clienteId).isEmpty());
    }

    @Test
    void debeRechazarDesactivarDosVeces() throws SQLException {
        Mascota creada = servicio.crear(Mascota.nueva(clienteId, "Firulais", null, null, null, null, null));
        servicio.desactivar(creada.id());

        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(creada.id()));
    }
}
