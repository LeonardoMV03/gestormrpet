package net.martinezoft.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256. El formato almacenado es
 * {@code pbkdf2$sha256$<iteraciones>$<saltBase64>$<hashBase64>} y la
 * verificación compara en tiempo constante.
 */
public final class PasswordHasher {

    public static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    public static final String PREFIJO = "pbkdf2";
    public static final String HASH = "sha256";
    public static final int ITERACIONES = 210_000;
    public static final int SAL_BYTES = 16;
    public static final int CLAVE_BITS = 256;

    private final SecureRandom aleatorio;

    public PasswordHasher() {
        this(new SecureRandom());
    }

    public PasswordHasher(SecureRandom aleatorio) {
        this.aleatorio = Objects.requireNonNull(aleatorio, "El generador de salt es obligatorio");
    }

    public String hash(String password) {
        Objects.requireNonNull(password, "La contraseña es obligatoria");
        byte[] sal = new byte[SAL_BYTES];
        aleatorio.nextBytes(sal);
        byte[] clave = derivar(password.toCharArray(), sal, ITERACIONES, CLAVE_BITS);
        return String.join("$",
                PREFIJO,
                HASH,
                Integer.toString(ITERACIONES),
                Base64.getEncoder().encodeToString(sal),
                Base64.getEncoder().encodeToString(clave));
    }

    /**
     * Verifica una contraseña contra un hash almacenado. Un hash con formato
     * inválido o corrupto devuelve {@code false} sin lanzar excepciones.
     */
    public boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$", -1);
        if (partes.length != 5 || !PREFIJO.equals(partes[0]) || !HASH.equals(partes[1])) {
            return false;
        }
        int iteracionesAlmacenadas;
        byte[] sal;
        byte[] esperado;
        try {
            iteracionesAlmacenadas = Integer.parseInt(partes[2]);
            sal = Base64.getDecoder().decode(partes[3]);
            esperado = Base64.getDecoder().decode(partes[4]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (iteracionesAlmacenadas < 1 || sal.length == 0 || esperado.length == 0) {
            return false;
        }
        byte[] calculado = derivar(password.toCharArray(), sal, iteracionesAlmacenadas, esperado.length * 8);
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo derivar la clave PBKDF2", e);
        } finally {
            spec.clearPassword();
        }
    }
}
