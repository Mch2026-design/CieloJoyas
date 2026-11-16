package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.DetallePedidoRepository;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import ica.joyas.maricielojoyas.validacion.MercadoPagoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Endpoints preparados para activar Mercado Pago (Checkout Pro / Tarjeta). */
@RestController
@RequestMapping("/api/payments/mercadopago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPago;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;

    public MercadoPagoController(
            MercadoPagoService mercadoPago,
            PedidoRepository pedidos,
            DetallePedidoRepository detalles) {
        this.mercadoPago = mercadoPago;
        this.pedidos = pedidos;
        this.detalles = detalles;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return mercadoPago.configPublica();
    }

    @PostMapping("/preference")
    public Map<String, Object> preference(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        if (!mercadoPago.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago está configurado pero deshabilitado (MERCADOPAGO_ENABLED=false).");
        }
        Object rawId = body == null ? null : body.get("orderId");
        if (rawId == null || !String.valueOf(rawId).matches("[0-9]{1,12}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId inválido.");
        }
        long orderId = Long.parseLong(String.valueOf(rawId));
        Pedido pedido = pedidos.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        return mercadoPago.crearPreferencia(pedido, detalles.findByPedidoIdOrderById(orderId), baseUrl);
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "x-signature", required = false) String signature) {
        return mercadoPago.procesarWebhook(payload, signature);
    }
}
