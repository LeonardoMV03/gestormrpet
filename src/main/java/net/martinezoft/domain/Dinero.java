package net.martinezoft.domain;

import java.math.BigDecimal;

/**
 * Conversión entre el texto que captura la persona usuaria y los centavos que
 * se persisten. Trabaja con {@link BigDecimal}; nunca con {@code float} ni
 * {@code double}.
 */
public final class Dinero {

    private static final String PATRON_PESOS = "\\d+(\\.\\d{1,2})?";

    private Dinero() {
    }

    public static long parsearCentavos(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        String limpio = texto.trim();
        if (limpio.startsWith("-")) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (limpio.chars().filter(caracter -> caracter == ',' || caracter == '.').count() > 1) {
            throw new IllegalArgumentException("El precio tiene un formato inválido");
        }
        String normalizado = limpio.replace(',', '.');
        if (!normalizado.matches(PATRON_PESOS)) {
            throw new IllegalArgumentException("El precio tiene un formato inválido");
        }
        try {
            return new BigDecimal(normalizado).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El precio es demasiado grande", e);
        }
    }

    public static String formatearCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2).toPlainString().replace('.', ',');
    }
}
