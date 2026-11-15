package ica.joyas.maricielojoyas;

import ica.joyas.maricielojoyas.validacion.ClaveAcceso;
import ica.joyas.maricielojoyas.validacion.Reglas;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidacionesTests {
    @Test void normalizaCorreosYConservaDireccionesConSubdominios() {
        assertEquals("ana+joyas@tienda.example.com", Reglas.email(Map.of("email", "  Ana+Joyas@Tienda.Example.COM  ")));
        assertEquals("a.b@c.pe", Reglas.email(Map.of("email", "a.b@c.pe")));
    }

    @Test void rechazaCorreosSinDominioSeparadorOSufijoYConEspacios() {
        for (String email : List.of("ana.example.com", "ana@@example.com", "@example.com", "ana@.pe",
                "ana@example", "ana@example.p", "ana @example.com", "ana@exam ple.com", "ana\t@example.com")) {
            assertThrows(ResponseStatusException.class, () -> Reglas.email(Map.of("email", email)), email);
        }
    }

    @Test void rechazaEntradasLargasSinRetrocesoCostoso() {
        String sinSeparador = "a@" + "a".repeat(252);
        String sinSufijo = "a@" + ".".repeat(250) + "a";
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (int i = 0; i < 1000; i++) {
                assertThrows(ResponseStatusException.class, () -> Reglas.email(Map.of("email", sinSeparador)));
            }
        });
        assertEquals(sinSufijo, Reglas.email(Map.of("email", sinSufijo)));
        assertThrows(ResponseStatusException.class, () -> Reglas.email(Map.of("email", "a@" + "a".repeat(253))));
    }

    @Test void cadaHashTieneSalPropiaYValidaSoloLaClaveCorrecta() {
        String primero = ClaveAcceso.guardar("Clave de prueba 2026");
        String segundo = ClaveAcceso.guardar("Clave de prueba 2026");
        assertNotEquals(primero, segundo);
        assertTrue(ClaveAcceso.coincide("Clave de prueba 2026", primero));
        assertTrue(ClaveAcceso.coincide("Clave de prueba 2026", segundo));
        assertFalse(ClaveAcceso.coincide("Otra clave", primero));
    }

    @Test void rechazaHashesInvalidosSinAceptarLaClave() {
        for (String hash : List.of("", "sin-formato", "pbkdf2$1$AA==$AA==", "pbkdf2$210000$?$$")) {
            assertFalse(ClaveAcceso.coincide("Clave de prueba 2026", hash));
        }
    }
}
