package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void debeExigirNombreNoVacio() {
        assertThrows(IllegalArgumentException.class, () -> Cliente.nuevo("  ", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> Cliente.nuevo(null, null, null, null, null));
    }

    @Test
    void debeRecortarElNombre() {
        assertEquals("Ana", Cliente.nuevo("  Ana  ", null, null, null, null).nombre());
    }

    @Test
    void debeConvertirCamposOpcionalesVaciosEnNulos() {
        Cliente cliente = Cliente.nuevo("Ana", " ", "", "\t", "");

        assertNull(cliente.telefono());
        assertNull(cliente.email());
        assertNull(cliente.direccion());
        assertNull(cliente.notas());
    }

    @Test
    void debeRecortarCamposOpcionalesInformados() {
        Cliente cliente = Cliente.nuevo("Ana", " 555  ", " ana@x.com ", " Calle 1 ", " nota ");

        assertEquals("555", cliente.telefono());
        assertEquals("ana@x.com", cliente.email());
        assertEquals("Calle 1", cliente.direccion());
        assertEquals("nota", cliente.notas());
    }
}
