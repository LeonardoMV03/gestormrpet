package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MascotaTest {

    @Test
    void debeExigirCliente() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Mascota.nueva(null, "Firulais", null, null, null, null, null));
        assertEquals("La mascota debe pertenecer a un cliente", error.getMessage());
    }

    @Test
    void debeExigirNombre() {
        assertThrows(IllegalArgumentException.class,
                () -> Mascota.nueva(1L, "  ", null, null, null, null, null));
    }

    @Test
    void debeUsarPerroPorEspeciePorDefecto() {
        assertEquals("perro", Mascota.nueva(1L, "Firulais", null, null, null, null, null).especie());
        assertEquals("gato", Mascota.nueva(1L, "Michi", "GATO", null, null, null, null).especie());
    }

    @Test
    void debeNormalizarElSexo() {
        assertEquals("hembra", Mascota.nueva(1L, "Luna", null, null, "Hembra", null, null).sexo());
        assertNull(Mascota.nueva(1L, "Luna", null, null, " ", null, null).sexo());
    }

    @Test
    void debeRechazarSexoInvalido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Mascota.nueva(1L, "Luna", null, null, "otro", null, null));
        assertEquals("El sexo debe ser macho o hembra", error.getMessage());
    }

    @Test
    void debeValidarYNormalizarLaFechaDeNacimiento() {
        assertEquals("2020-05-10",
                Mascota.nueva(1L, "Firulais", null, null, null, " 2020-05-10 ", null).fechaNacimiento());
        assertNull(Mascota.nueva(1L, "Firulais", null, null, null, "", null).fechaNacimiento());
        assertThrows(IllegalArgumentException.class,
                () -> Mascota.nueva(1L, "Firulais", null, null, null, "10/05/2020", null));
    }
}
