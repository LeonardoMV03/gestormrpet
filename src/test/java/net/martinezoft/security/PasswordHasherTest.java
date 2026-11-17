package net.martinezoft.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private static final String FORMATO_ESPERADO =
            "^pbkdf2\\$sha256\\$210000\\$[A-Za-z0-9+/]+={0,2}\\$[A-Za-z0-9+/]+={0,2}$";

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void debeGenerarUnHashConElFormatoEsperado() {
        String hash = hasher.hash("secreto123");

        assertTrue(hash.matches(FORMATO_ESPERADO), "Formato inesperado: " + hash);
        assertTrue(hash.startsWith("pbkdf2$sha256$210000$"), "Debe usar PBKDF2-SHA256 con 210000 iteraciones");
    }

    @Test
    void debeVerificarLaContrasenaCorrecta() {
        String hash = hasher.hash("secreto123");

        assertTrue(hasher.verificar("secreto123", hash));
    }

    @Test
    void debeRechazarLaContrasenaIncorrecta() {
        String hash = hasher.hash("secreto123");

        assertFalse(hasher.verificar("secreto124", hash));
        assertFalse(hasher.verificar("", hash));
        assertFalse(hasher.verificar("Secreto123", hash));
    }

    @Test
    void debeUsarUnaSalDistintaEnCadaHash() {
        String primero = hasher.hash("secreto123");
        String segundo = hasher.hash("secreto123");

        assertNotEquals(primero, segundo, "Dos hashes de la misma contraseña deben diferir por la sal");
        assertTrue(hasher.verificar("secreto123", primero));
        assertTrue(hasher.verificar("secreto123", segundo));
    }

    @Test
    void debeDevolverFalseAnteUnHashMalformado() {
        List<String> malformados = List.of(
                "",
                "texto plano",
                "pbkdf2$sha256",
                "pbkdf2$sha256$210000$no-es-base64!$AAAA",
                "pbkdf2$sha256$210000$AAAA$no-es-base64!",
                "pbkdf2$sha256$abc$AAAA$AAAA",
                "pbkdf2$sha256$0$AAAA$AAAA",
                "pbkdf2$sha1$210000$AAAA$AAAA",
                "bcrypt$sha256$210000$AAAA$AAAA",
                "pbkdf2$sha256$210000$$");

        for (String malformado : malformados) {
            assertFalse(hasher.verificar("secreto123", malformado),
                    "Un hash malformado debe devolver false: " + malformado);
        }
    }

    @Test
    void debeDevolverFalseConValoresNulos() {
        String hash = hasher.hash("secreto123");

        assertFalse(hasher.verificar(null, hash));
        assertFalse(hasher.verificar("secreto123", null));
    }

    @Test
    void noDebeAlmacenarLaContrasenaEnTextoPlano() {
        String hash = hasher.hash("secreto123");

        assertFalse(hash.contains("secreto123"), "El hash no debe contener la contraseña");
    }

    @Test
    void debeCumplirLosParametrosMinimosDeSeguridad() {
        String[] partes = hasher.hash("secreto123").split("\\$", -1);

        int iteraciones = Integer.parseInt(partes[2]);
        byte[] sal = Base64.getDecoder().decode(partes[3]);
        byte[] clave = Base64.getDecoder().decode(partes[4]);

        assertTrue(iteraciones >= 210_000, "Debe usar al menos 210000 iteraciones: " + iteraciones);
        assertEquals(PasswordHasher.SAL_BYTES, sal.length, "El salt debe ser de 16 bytes");
        assertEquals(PasswordHasher.CLAVE_BITS / 8, clave.length, "La clave derivada debe ser de 256 bits");
    }
}
