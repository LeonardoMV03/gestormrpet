package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DineroTest {

    @Test
    void debeParsearPesosEnteros() {
        assertEquals(15000, Dinero.parsearCentavos("150"));
        assertEquals(1, Dinero.parsearCentavos("0,01"));
    }

    @Test
    void debeAceptarComaComoSeparadorDecimal() {
        assertEquals(15050, Dinero.parsearCentavos("150,5"));
        assertEquals(15050, Dinero.parsearCentavos("150,50"));
    }

    @Test
    void debeAceptarPuntoComoSeparadorDecimal() {
        assertEquals(15050, Dinero.parsearCentavos("150.5"));
        assertEquals(15050, Dinero.parsearCentavos("150.50"));
    }

    @Test
    void debeParsearCeroConYSinDecimales() {
        assertEquals(0, Dinero.parsearCentavos("0"));
        assertEquals(0, Dinero.parsearCentavos("0,00"));
    }

    @Test
    void debeRechazarTextoVacioONulo() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos(null));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos(""));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("   "));
    }

    @Test
    void debeRechazarTextoNoNumerico() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("abc"));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("150 pesos"));
    }

    @Test
    void debeRechazarPrecioNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("-1"));
    }

    @Test
    void debeRechazarMasDeDosDecimales() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("1,234"));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("1.234"));
    }

    @Test
    void debeRechazarMultiplesSeparadores() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("1,2,3"));
    }

    @Test
    void debeRechazarPrecioDesbordado() {
        assertThrows(IllegalArgumentException.class,
                () -> Dinero.parsearCentavos("99999999999999999999"));
    }

    @Test
    void debeFormatearCentavosConComaDecimal() {
        assertEquals("150,50", Dinero.formatearCentavos(15050));
        assertEquals("1,50", Dinero.formatearCentavos(150));
        assertEquals("0,00", Dinero.formatearCentavos(0));
    }

    @Test
    void debeHacerIdaYVueltaSinPerderCentavos() {
        assertEquals(15050, Dinero.parsearCentavos(Dinero.formatearCentavos(15050)));
        assertEquals("150,50", Dinero.formatearCentavos(Dinero.parsearCentavos("150,5")));
        assertEquals("0,05", Dinero.formatearCentavos(Dinero.parsearCentavos("0,05")));
        assertEquals(5, Dinero.parsearCentavos(Dinero.formatearCentavos(5)));
    }

    @Test
    void debeIgnorarEspaciosAlrededorDelPrecio() {
        assertEquals(15050, Dinero.parsearCentavos("  150,50  "));
        assertEquals(0, Dinero.parsearCentavos("\t0\n"));
    }

    @Test
    void debeRechazarSeparadorSinDecimales() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("150,"));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("150."));
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos(","));
    }

    @Test
    void debeRechazarSignoPositivoExplicito() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.parsearCentavos("+150"));
    }
}
