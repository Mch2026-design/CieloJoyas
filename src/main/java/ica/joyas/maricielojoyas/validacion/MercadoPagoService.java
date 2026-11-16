package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.DetallePedido;
import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración preparada para Mercado Pago (Checkout Pro / Tarjeta).
 * Permanece deshabilitada mientras {@code cielo.mercadopago.enabled=false} o falte {@code access-token}.
 */
@Service
public class MercadoPagoService {

    private static final String MP_API_BASE = "https://api.mercadopago.com";

    private final boolean enabled;
    private final boolean sandbox;
    private final String publicKey;
    private final String accessToken;
    private final String webhookSecret;
    private final PedidoRepository pedidos;
    private final RestClient http;

    public MercadoPagoService(
            @Value("${cielo.mercadopago.enabled:false}") boolean enabled,
            @Value("${cielo.mercadopago.sandbox:true}") boolean sandbox,
            @Value("${cielo.mercadopago.public-key:}") String publicKey,
            @Value("${cielo.mercadopago.access-token:}") String accessToken,
            @Value("${cielo.mercadopago.webhook-secret:}") String webhookSecret,
            PedidoRepository pedidos) {
        this.enabled = enabled;
        this.sandbox = sandbox;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.pedidos = pedidos;
        this.http = RestClient.builder()
                .baseUrl(MP_API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isEnabled() {
        return enabled && !accessToken.isBlank();
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public Map<String, Object> configPublica() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", "mercadopago");
        out.put("enabled", isEnabled());
        out.put("sandbox", sandbox);
        out.put("publicKey", isEnabled() ? publicKey : "");
        return out;
    }

    /** Crea una preferencia Checkout Pro en Mercado Pago para el pedido indicado. */
    public Map<String, Object> crearPreferencia(Pedido pedido, List<DetallePedido> detalles, String baseUrl) {
        if (!isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago está configurado pero deshabilitado (MERCADOPAGO_ENABLED=false).");
        }
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado.");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        if (detalles != null && !detalles.isEmpty()) {
            for (DetallePedido d : detalles) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", d.getNombreProducto());
                item.put("quantity", d.getCantidad());
                item.put("currency_id", "PEN");
                item.put("unit_price", d.getPrecioUnitario());
                items.add(item);
            }
        } else {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", "Pedido Cielo Joyas #" + pedido.getId());
            item.put("quantity", 1);
            item.put("currency_id", "PEN");
            item.put("unit_price", pedido.getTotal());
            items.add(item);
        }

        String origin = baseUrl == null || baseUrl.isBlank() ? "https://cielojoyas.onrender.com" : baseUrl.replaceAll("/+$", "");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("external_reference", String.valueOf(pedido.getId()));
        body.put("back_urls", Map.of(
                "success", origin + "/cuenta/cuenta.html?pago=aprobado&pedido=" + pedido.getId(),
                "failure", origin + "/tienda/index.html?pago=rechazado&pedido=" + pedido.getId(),
                "pending", origin + "/cuenta/cuenta.html?pago=pendiente&pedido=" + pedido.getId()
        ));
        body.put("auto_return", "approved");
        body.put("notification_url", origin + "/api/payments/mercadopago/webhook");

        Map<String, Object> mpRes = http.post()
                .uri("/checkout/preferences")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preferenceId", mpRes == null ? "" : mpRes.getOrDefault("id", ""));
        result.put("initPoint", mpRes == null ? "" : mpRes.getOrDefault("init_point", ""));
        result.put("sandboxInitPoint", mpRes == null ? "" : mpRes.getOrDefault("sandbox_init_point", ""));
        result.put("sandbox", sandbox);
        return result;
    }

    /** Endpoint listo para recibir notificaciones webhook de Mercado Pago al habilitar la pasarela. */
    @Transactional
    public Map<String, Object> procesarWebhook(Map<String, Object> payload, String signatureHeader) {
        if (!isEnabled()) {
            return Map.of("ok", false, "enabled", false, "message", "Mercado Pago deshabilitado.");
        }
        if (!webhookSecret.isBlank() && (signatureHeader == null || signatureHeader.isBlank())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de webhook ausente.");
        }
        Object dataObj = payload == null ? null : payload.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap) || dataMap.get("id") == null) {
            return Map.of("ok", true, "ignored", true);
        }
        String paymentId = String.valueOf(dataMap.get("id"));
        if (!paymentId.matches("[0-9]{1,30}")) {
            return Map.of("ok", true, "ignored", true);
        }
        Map<String, Object> payment = http.get()
                .uri("/v1/payments/{id}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (payment == null) {
            return Map.of("ok", false, "message", "Pago no encontrado.");
        }
        String status = String.valueOf(payment.getOrDefault("status", ""));
        String extRef = String.valueOf(payment.getOrDefault("external_reference", ""));
        if ("approved".equalsIgnoreCase(status) && extRef.matches("[0-9]{1,12}")) {
            long orderId = Long.parseLong(extRef);
            pedidos.findById(orderId).ifPresent(p -> {
                if ("pendiente".equalsIgnoreCase(p.getEstado())) {
                    p.setEstado("pagado");
                    pedidos.save(p);
                }
            });
            return Map.of("ok", true, "orderId", orderId, "status", "pagado");
        }
        return Map.of("ok", true, "status", status);
    }
}
