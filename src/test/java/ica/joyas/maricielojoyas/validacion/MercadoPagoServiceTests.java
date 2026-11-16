package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MercadoPagoServiceTests {

    @Test
    void deshabilitadoPorDefectoNoExponeLlaveYBloqueaPreferencia() {
        PedidoRepository repo = mock(PedidoRepository.class);
        MercadoPagoService service = new MercadoPagoService(false, true, "APP_USR-pub", "APP_USR-sec", "", repo);

        assertFalse(service.isEnabled());
        Map<String, Object> cfg = service.configPublica();
        assertEquals(false, cfg.get("enabled"));
        assertEquals("", cfg.get("publicKey"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPreferencia(new Pedido(), List.of(), "http://localhost:8080"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());

        Map<String, Object> wh = service.procesarWebhook(Map.of("data", Map.of("id", "123")), null);
        assertEquals(false, wh.get("enabled"));
    }

    @Test
    void sinAccessTokenPermaneceDeshabilitado() {
        PedidoRepository repo = mock(PedidoRepository.class);
        MercadoPagoService service = new MercadoPagoService(true, true, "APP_USR-pub", "   ", "", repo);

        assertFalse(service.isEnabled());
    }
}
