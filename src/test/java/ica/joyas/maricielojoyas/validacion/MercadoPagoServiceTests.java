package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MercadoPagoServiceTests {

    @Test
    void deshabilitadoPorDefectoNoExponeLlaveYBloqueaPreferenciaYPago() {
        PedidoRepository repo = mock(PedidoRepository.class);
        MercadoPagoService service = new MercadoPagoService(false, true, "APP_USR-pub", "APP_USR-sec", "", repo);

        assertFalse(service.isEnabled());
        Map<String, Object> cfg = service.configPublica();
        assertEquals(false, cfg.get("enabled"));
        assertEquals("", cfg.get("publicKey"));

        ResponseStatusException exPref = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPreferencia(new Pedido(), List.of(), "http://localhost:8080"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exPref.getStatusCode());

        ResponseStatusException exPay = assertThrows(
                ResponseStatusException.class,
                () -> service.pagarConTarjeta(new Pedido(), "tok_123", "visa", "", 1, "DNI", "12345678", "a@b.com"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exPay.getStatusCode());

        Map<String, Object> wh = service.procesarWebhook(Map.of("data", Map.of("id", "123")), null);
        assertEquals(false, wh.get("enabled"));
    }

    @Test
    void sinAccessTokenPermaneceDeshabilitado() {
        PedidoRepository repo = mock(PedidoRepository.class);
        MercadoPagoService service = new MercadoPagoService(true, true, "APP_USR-pub", "   ", "", repo);

        assertFalse(service.isEnabled());
    }

    @Test
    void habilitadoExponePublicKeyYValidaEstadoYTarjeta() {
        PedidoRepository repo = mock(PedidoRepository.class);
        MercadoPagoService service = new MercadoPagoService(true, true, " TEST-pub-123 ", " TEST-sec-123 ", "", repo);

        assertTrue(service.isEnabled());
        Map<String, Object> cfg = service.configPublica();
        assertEquals(true, cfg.get("enabled"));
        assertEquals(true, cfg.get("sandbox"));
        assertEquals("TEST-pub-123", cfg.get("publicKey"));

        Pedido pedido = new Pedido();
        pedido.setEstado("pendiente");
        pedido.setTotal(new BigDecimal("50.00"));
        ResponseStatusException exToken = assertThrows(
                ResponseStatusException.class,
                () -> service.pagarConTarjeta(pedido, "  ", "visa", "", 1, "DNI", "12345678", "a@b.com"));
        assertEquals(HttpStatus.BAD_REQUEST, exToken.getStatusCode());
    }
}
