package net.martinezoft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Mascota;

class MascotaRepositoryTest {

    @TempDir
    Path tempDir;

    private ClienteRepository clientes;
    private MascotaRepository repositorio;
    private Long clienteId;

    @BeforeEach
    void prepararRepositorio() throws SQLException {
        Database database = new Database(tempDir.resolve("mascotas.db"));
        new DatabaseInitializer(database).migrar();
        clientes = new ClienteRepository(database);
        repositorio = new MascotaRepository(database);
        clienteId = clientes.insertar(Cliente.nuevo("Ana", null, null, null, null)).id();
    }

    @Test
    void debeInsertarYBuscarPorId() throws SQLException {
        Mascota guardada = repositorio.insertar(
                Mascota.nueva(clienteId, "Firulais", "perro", "Labrador", "macho", "2020-05-10", "Tranquilo"));

        assertNotNull(guardada.id());
        Mascota encontrada = repositorio.buscarPorId(guardada.id()).orElseThrow();
        assertEquals(guardada, encontrada);
        assertTrue(encontrada.activo());
    }

    @Test
    void debeListarSoloMascotasActivasDelClienteOrdenadasPorNombre() throws SQLException {
        Long otroCliente = clientes.insertar(Cliente.nuevo("Bruno", null, null, null, null)).id();
        Mascota zoe = repositorio.insertar(Mascota.nueva(clienteId, "Zoe", "gato", null, "hembra", null, null));
        Mascota ana = repositorio.insertar(Mascota.nueva(clienteId, "Ana", "perro", null, null, null, null));
        repositorio.insertar(Mascota.nueva(otroCliente, "Otra", "perro", null, null, null, null));

        repositorio.desactivar(zoe.id());

        List<Mascota> mascotas = repositorio.listarPorCliente(clienteId);

        assertEquals(List.of(ana), mascotas);
    }

    @Test
    void debeListarVacioCuandoElClienteNoTieneMascotas() throws SQLException {
        assertTrue(repositorio.listarPorCliente(clienteId).isEmpty());
    }

    @Test
    void debeActualizarUnaMascotaActiva() throws SQLException {
        Mascota guardada = repositorio.insertar(
                Mascota.nueva(clienteId, "Firulais", "perro", null, "macho", null, null));
        Mascota editada = new Mascota(guardada.id(), clienteId, "Firulais", "perro", "Beagle",
                "macho", "2021-01-01", "Muy juguetón", true);

        assertTrue(repositorio.actualizar(editada));

        assertEquals(editada, repositorio.buscarPorId(guardada.id()).orElseThrow());
    }

    @Test
    void debeDesactivarConSoftDelete() throws SQLException {
        Mascota guardada = repositorio.insertar(
                Mascota.nueva(clienteId, "Firulais", "perro", null, null, null, null));

        assertTrue(repositorio.desactivar(guardada.id()));

        assertTrue(repositorio.listarPorCliente(clienteId).isEmpty());
        assertFalse(repositorio.buscarPorId(guardada.id()).orElseThrow().activo());
    }

    @Test
    void noDebeActualizarUnaMascotaInactiva() throws SQLException {
        Mascota guardada = repositorio.insertar(
                Mascota.nueva(clienteId, "Firulais", "perro", null, null, null, null));
        repositorio.desactivar(guardada.id());
        Mascota editada = new Mascota(guardada.id(), clienteId, "Firulais", "perro", null,
                null, null, null, true);

        assertFalse(repositorio.actualizar(editada));
    }

    @Test
    void debeDevolverVacioAlBuscarUnaMascotaInexistente() throws SQLException {
        assertTrue(repositorio.buscarPorId(999).isEmpty());
    }

    @Test
    void debeFallarAlInsertarUnaMascotaDeUnClienteInexistente() {
        Mascota huerfana = Mascota.nueva(999L, "Firulais", "perro", null, null, null, null);

        assertThrows(SQLException.class, () -> repositorio.insertar(huerfana));
    }

    @Test
    void debePersistirCamposOpcionalesNulos() throws SQLException {
        Mascota guardada = repositorio.insertar(
                Mascota.nueva(clienteId, "Firulais", "perro", null, null, null, null));

        Mascota encontrada = repositorio.buscarPorId(guardada.id()).orElseThrow();
        assertNull(encontrada.raza());
        assertNull(encontrada.sexo());
        assertNull(encontrada.fechaNacimiento());
        assertNull(encontrada.notas());
    }
}
