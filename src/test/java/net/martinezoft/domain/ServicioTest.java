package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServicioTest {

    @Test
    void debeRechazarNombreNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Servicio(null, null, null, 0, null, true));
    }

    @Test
    void debeRechazarNombreEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new Servicio(null, "   ", null, 0, null, true));
    }

    @Test
    void debeRecortarNombreYDescripcion() {
        Servicio servicio = new Servicio(null, "  Baño  ", "  Baño completo  ", 15050, 45, true);

        assertEquals("Baño", servicio.nombre());
        assertEquals("Baño completo", servicio.descripcion());
    }

    @Test
    void debeConvertirDescripcionEnBlancoANull() {
        Servicio servicio = new Servicio(null, "Baño", "   ", 0, null, true);

        assertNull(servicio.descripcion());
    }

    @Test
    void debeRechazarPrecioNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Servicio(null, "Baño", null, -1, null, true));
    }

    @Test
    void debeRechazarDuracionNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new Servicio(null, "Baño", null, 0, -5, true));
    }

    @Test
    void debePermitirPrecioCeroYDuracionNula() {
        Servicio servicio = new Servicio(null, "Baño", null, 0, null, true);

        assertEquals(0, servicio.precioCentavos());
        assertNull(servicio.duracionMin());
    }

    @Test
    void debeCrearServicioNuevoActivoYSinId() {
        Servicio servicio = Servicio.nuevo("Baño", "Baño completo", 15050, 45);

        assertNull(servicio.id());
        assertTrue(servicio.activo());
        assertFalse(servicio.nombre().isBlank());
        assertEquals("Baño", servicio.nombre(), "nuevo debe recortar el nombre");
        assertEquals("Baño completo", servicio.descripcion());
    }

    @Test
    void debePermitirDuracionCero() {
        Servicio servicio = new Servicio(null, "Consulta", null, 0, 0, true);

        assertEquals(0, servicio.duracionMin().intValue());
    }

    @Test
    void debeConservarDescripcionNula() {
        Servicio servicio = new Servicio(null, "Baño", null, 0, null, true);

        assertNull(servicio.descripcion());
    }
}
