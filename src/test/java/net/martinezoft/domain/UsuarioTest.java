package net.martinezoft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final String HASH = "pbkdf2$100000$c2FsRGVQcnVlYmE$aGFzaFNlY3JldG9EZVBydWViYQ";

    @Test
    void toStringNoDebeExponerElHash() {
        Usuario usuario = new Usuario(1L, "admin", HASH, true);

        String texto = usuario.toString();

        assertFalse(texto.contains(HASH), "toString no debe contener el hash");
        assertFalse(texto.contains("aGFzaFNlY3JldG9EZVBydWViYQ"), "toString no debe contener fragmentos del hash");
        assertTrue(texto.contains("admin"));
    }

    @Test
    void toStringDebeIncluirSoloCamposNoSensibles() {
        Usuario usuario = new Usuario(1L, "admin", HASH, true);

        assertEquals("Usuario[id=1, usuario=admin, debeCambiarPassword=true]", usuario.toString());
    }
}
