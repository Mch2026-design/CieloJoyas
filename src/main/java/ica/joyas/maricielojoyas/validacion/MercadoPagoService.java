package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.DetallePedido;
import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Integración con Mercado Pago (Checkout API con tarjeta y Checkout Pro).
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
    private final PedidoService pedidosService;
    private final RestClient http;

    public MercadoPagoService(
            boolean enabled,
            boolean sandbox,
            String publicKey,
            String accessToken,
            String webhookSecret,
            PedidoRepository pedidos) {
        this(enabled, sandbox, publicKey, accessToken, webhookSecret, pedidos, null);
    }

    @Autowired
    public MercadoPagoService(
            @Value("${cielo.mercadopago.enabled:false}") boolean enabled,
            @Value("${cielo.mercadopago.sandbox:true}") boolean sandbox,
            @Value("${cielo.mercadopago.public-key:}") String publicKey,
            @Value("${cielo.mercadopago.access-token:}") String accessToken,
            @Value("${cielo.mercadopago.webhook-secret:}") String webhookSecret,
            PedidoRepository pedidos,
            @Lazy PedidoService pedidosService) {
        this.enabled = enabled;
        this.sandbox = sandbox;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.pedidos = pedidos;
        this.pedidosService = pedidosService;
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
        if (origin.startsWith("https://")) {
            body.put("notification_url", origin + "/api/payments/mercadopago/webhook");
        }

        Map<String, Object> mpRes;
        try {
            mpRes = http.post()
                    .uri("/checkout/preferences")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago rechazó la preferencia (" + ex.getStatusCode().value() + "). Verifica tus credenciales.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preferenceId", mpRes == null ? "" : mpRes.getOrDefault("id", ""));
        result.put("initPoint", mpRes == null ? "" : mpRes.getOrDefault("init_point", ""));
        result.put("sandboxInitPoint", mpRes == null ? "" : mpRes.getOrDefault("sandbox_init_point", ""));
        result.put("sandbox", sandbox);
        return result;
    }

    /** Procesa un cobro directo con tarjeta tokenizada mediante Checkout API ({@code /v1/payments}). */
    @Transactional
    public Map<String, Object> pagarConTarjeta(
            Pedido pedido,
            String token,
            String paymentMethodId,
            String issuerId,
            int installments,
            String identificationType,
            String identificationNumber,
            String payerEmail) {
        if (!isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago está configurado pero deshabilitado (MERCADOPAGO_ENABLED=false).");
        }
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado.");
        }
        if (!"pendiente".equalsIgnoreCase(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ya fue procesado.");
        }
        String cleanToken = token == null ? "" : token.trim();
        if (cleanToken.isBlank() || cleanToken.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de tarjeta inválido.");
        }
        String cleanMethod = paymentMethodId == null ? "" : paymentMethodId.trim().toLowerCase(Locale.ROOT);
        if (!cleanMethod.matches("[a-z0-9_]{2,30}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pudimos identificar el tipo de tarjeta.");
        }
        int safeInstallments = Math.max(1, Math.min(24, installments));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", pedido.getTotal());
        body.put("token", cleanToken);
        body.put("description", "Pedido Cielo Joyas #" + pedido.getId());
        body.put("installments", safeInstallments);
        body.put("payment_method_id", cleanMethod);
        if (issuerId != null && !issuerId.isBlank() && issuerId.trim().matches("[0-9]{1,20}")) {
            body.put("issuer_id", Long.parseLong(issuerId.trim()));
        }
        body.put("external_reference", String.valueOf(pedido.getId()));

        Map<String, Object> payer = new LinkedHashMap<>();
        String email = (payerEmail != null && !payerEmail.isBlank()) ? payerEmail.trim() : pedido.getEmail();
        payer.put("email", email);
        if (identificationNumber != null && !identificationNumber.isBlank()) {
            String idType = (identificationType == null || identificationType.isBlank())
                    ? "DNI"
                    : identificationType.trim().toUpperCase(Locale.ROOT);
            payer.put("identification", Map.of("type", idType, "number", identificationNumber.trim()));
        }
        body.put("payer", payer);

        String idempotencyKey = "cielo-order-" + pedido.getId() + "-" + cleanToken;
        Map<String, Object> mpRes;
        try {
            mpRes = http.post()
                    .uri("/v1/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo procesar el pago con Mercado Pago: revisa los datos de la tarjeta o tus credenciales de prueba.");
        }

        if (mpRes == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió respuesta.");
        }
        String mpStatus = String.valueOf(mpRes.getOrDefault("status", ""));
        String detail = String.valueOf(mpRes.getOrDefault("status_detail", ""));
        Object paymentId = mpRes.getOrDefault("id", "");

        if ("approved".equalsIgnoreCase(mpStatus)) {
            confirmarPedidoPagado(pedido.getId());
            return Map.of(
                    "ok", true,
                    "paymentId", String.valueOf(paymentId),
                    "mpStatus", mpStatus,
                    "mpStatusDetail", detail,
                    "orderStatus", "pagado"
            );
        }
        if ("in_process".equalsIgnoreCase(mpStatus) || "pending".equalsIgnoreCase(mpStatus) || "authorized".equalsIgnoreCase(mpStatus)) {
            return Map.of(
                    "ok", true,
                    "paymentId", String.valueOf(paymentId),
                    "mpStatus", mpStatus,
                    "mpStatusDetail", detail,
                    "orderStatus", pedido.getEstado()
            );
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeRechazo(detail));
    }

    /** Endpoint para recibir notificaciones webhook de Mercado Pago. */
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
            confirmarPedidoPagado(orderId);
            return Map.of("ok", true, "orderId", orderId, "status", "pagado");
        }
        return Map.of("ok", true, "status", status);
    }

    private void confirmarPedidoPagado(long orderId) {
        if (pedidosService != null) {
            pedidos.findById(orderId).ifPresent(p -> {
                if ("pendiente".equalsIgnoreCase(p.getEstado())) {
                    pedidosService.cambiarEstado(orderId, "pagado");
                }
            });
            return;
        }
        pedidos.findById(orderId).ifPresent(p -> {
            if ("pendiente".equalsIgnoreCase(p.getEstado())) {
                p.setEstado("pagado");
                pedidos.save(p);
            }
        });
    }

    private String mensajeRechazo(String detail) {
        String key = detail == null ? "" : detail.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "cc_rejected_bad_filled_card_number" -> "Revisa el número de tarjeta.";
            case "cc_rejected_bad_filled_date" -> "Revisa la fecha de vencimiento de tu tarjeta.";
            case "cc_rejected_bad_filled_security_code" -> "Revisa el código de seguridad (CVV).";
            case "cc_rejected_bad_filled_other" -> "Revisa los datos ingresados de tu tarjeta.";
            case "cc_rejected_insufficient_amount" -> "Tu tarjeta no cuenta con fondos suficientes.";
            case "cc_rejected_call_for_authorize" -> "Debes autorizar el pago con el banco emisor de tu tarjeta.";
            case "cc_rejected_card_disabled" -> "Tu tarjeta está inhabilitada para compras por internet. Actívala con tu banco.";
            case "cc_rejected_duplicated_payment" -> "Ya existe un pago reciente por este monto. Espera unos minutos antes de reintentar.";
            case "cc_rejected_high_risk" -> "El pago fue rechazado por validación de seguridad. Prueba con otra tarjeta o usa Yape/Plin.";
            case "cc_rejected_max_attempts" -> "Superaste el límite de intentos con esta tarjeta. Prueba con otro medio de pago.";
            default -> "Mercado Pago rechazó el pago (" + (key.isBlank() ? "tarjeta no autorizada" : key) + "). Intenta con otra tarjeta.";
        };
    }
}
