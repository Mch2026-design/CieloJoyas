package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.repositorio.DetallePedidoRepository;
import ica.joyas.maricielojoyas.repositorio.PedidoRepository;
import ica.joyas.maricielojoyas.validacion.AccesoService;
import ica.joyas.maricielojoyas.validacion.MercadoPagoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

/** Endpoints para pagos con Mercado Pago (Checkout API / Checkout Pro). */
@RestController
@RequestMapping("/api/payments/mercadopago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPago;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final AccesoService acceso;

    public MercadoPagoController(
            MercadoPagoService mercadoPago,
            PedidoRepository pedidos,
            DetallePedidoRepository detalles,
            AccesoService acceso) {
        this.mercadoPago = mercadoPago;
        this.pedidos = pedidos;
        this.detalles = detalles;
        this.acceso = acceso;
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
        Pedido pedido = pedidoPermitido(body == null ? null : body.get("orderId"), request);
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        return mercadoPago.crearPreferencia(pedido, detalles.findByPedidoIdOrderById(pedido.getId()), baseUrl);
    }

    @PostMapping("/pay")
    public Map<String, Object> pay(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        if (!mercadoPago.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago está configurado pero deshabilitado (MERCADOPAGO_ENABLED=false).");
        }
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de pago ausentes.");
        }
        Pedido pedido = pedidoPermitido(body.get("orderId"), request);
        String token = body.get("token") instanceof String s ? s : "";
        String paymentMethodId = body.get("paymentMethodId") instanceof String s ? s : "";
        String issuerId = body.get("issuerId") == null ? "" : String.valueOf(body.get("issuerId"));
        int installments = 1;
        if (body.get("installments") instanceof Number n) {
            installments = n.intValue();
        } else if (body.get("installments") instanceof String s && s.matches("[0-9]{1,2}")) {
            installments = Integer.parseInt(s);
        }
        String idType = body.get("identificationType") instanceof String s ? s : "DNI";
        String idNumber = body.get("identificationNumber") instanceof String s ? s : "";
        return mercadoPago.pagarConTarjeta(
                pedido, token, paymentMethodId, issuerId, installments, idType, idNumber, pedido.getEmail());
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "x-signature", required = false) String signature) {
        return mercadoPago.procesarWebhook(payload, signature);
    }

    private Pedido pedidoPermitido(Object rawId, HttpServletRequest request) {
        if (rawId == null || !String.valueOf(rawId).matches("[0-9]{1,12}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId inválido.");
        }
        long orderId = Long.parseLong(String.valueOf(rawId));
        Pedido pedido = pedidos.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        HttpSession sesion = request.getSession(false);
        Usuario usuario = acceso.actual(sesion);
        boolean propietario = usuario != null && (usuario.getRol().equals("ADMIN")
                || (pedido.getUsuario() != null && pedido.getUsuario().getId().equals(usuario.getId())));
        boolean compraDeSesion = sesion != null
                && sesion.getAttribute("pedidosSesion") instanceof Set<?> ids && ids.contains(orderId);
        if (!propietario && !compraDeSesion) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado.");
        }
        return pedido;
    }
}
