package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Pedido;
import ica.joyas.maricielojoyas.repositorio.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Envía la confirmación después de guardar la compra. Un fallo SMTP no cancela el pedido. */
@Service
public class AvisosPedido {
    private static final Logger log = LoggerFactory.getLogger(AvisosPedido.class);
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final PagoRepository pagos;
    private final ObjectProvider<JavaMailSender> remitentes;
    private final boolean habilitado;
    private final String desde;

    public AvisosPedido(PedidoRepository pedidos, DetallePedidoRepository detalles, PagoRepository pagos,
            ObjectProvider<JavaMailSender> remitentes,
            @Value("${cielo.mail.enabled:false}") boolean habilitado,
            @Value("${cielo.mail.from:}") String desde) {
        this.pedidos=pedidos; this.detalles=detalles; this.pagos=pagos;
        this.remitentes=remitentes; this.habilitado=habilitado; this.desde=desde;
    }

    @Transactional
    public String enviar(Long id) {
        // Serializa reintentos ordinarios para evitar dos avisos del mismo pedido.
        Pedido pedido=pedidos.bloquearPorId(id).orElseThrow();
        if (pedido.getCorreoEnviadoEn()!=null) return "sent";
        if (!habilitado || desde.isBlank()) return "not_configured";
        JavaMailSender remitente=remitentes.getIfAvailable();
        if (remitente==null) return "not_configured";
        try {
            remitente.send(mensaje(remitente,pedido));
        } catch (MailException | MessagingException | UnsupportedEncodingException | IllegalArgumentException error) {
            // No registrar credenciales, destinatarios ni contenido del mensaje.
            log.warn("No se pudo enviar el correo del pedido #{} ({})",id,error.getClass().getSimpleName());
            return "failed";
        }
        pedido.setCorreoEnviadoEn(OffsetDateTime.now());
        pedidos.save(pedido);
        return "sent";
    }

    private MimeMessage mensaje(JavaMailSender remitente, Pedido p)
            throws MessagingException, UnsupportedEncodingException {
        var pago=pagos.findByPedidoId(p.getId()).orElseThrow();
        String metodo=switch(pago.getMetodo()) {
            case "yape" -> "Yape"; case "plin" -> "Plin";
            case "transferencia" -> "Transferencia bancaria"; default -> "Coordinado con la tienda";
        };
        String estado=p.getEstado().equals("cancelado") ? "Pedido cancelado. No realices un nuevo pago."
            : pago.getEstado().equals("confirmado") ? "Tu pago está confirmado por la tienda."
            : "El pago está pendiente de verificación por la tienda. Registrar el pedido no realiza un cobro automático.";
        String direccion=Stream.of(p.getDireccion(),p.getDistrito(),p.getProvincia(),p.getDepartamento())
            .filter(v->v!=null&&!v.isBlank()).collect(Collectors.joining(", "));
        StringBuilder texto=new StringBuilder("CIELO JOYAS\n\n¡Gracias, ").append(p.getNombres())
            .append("!\nHemos recibido tu pedido #").append(p.getId()).append(".\n\n");
        StringBuilder html=new StringBuilder("""
            <!doctype html><html lang="es"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0;padding:24px 16px;background:#fff;color:#292622;font-family:Arial,Helvetica,sans-serif;font-size:16px;line-height:1.45;overflow-wrap:anywhere">
            <table role="presentation" style="width:100%;max-width:600px;margin:0 auto;border-collapse:collapse"><tr><td>
            <p style="margin:0 0 32px;letter-spacing:0.12em;font-size:18px;color:#916451">CIELO JOYAS</p>
            <h1 style="font-family:Georgia,serif;font-size:34px;line-height:1.2;font-weight:400;margin:0 0 28px">¡Gracias, 
            """).append(esc(p.getNombres())).append("!</h1><p style=\"margin:0 0 24px\">Hemos recibido tu pedido <strong>#")
            .append(p.getId()).append(".</strong></p><table aria-label=\"Resumen del pedido\" style=\"width:100%;border-collapse:collapse;font-variant-numeric:tabular-nums\">")
            .append("<thead><tr><th scope=\"col\" style=\"text-align:left;padding:0 0 12px;font-size:14px;color:#6d635d\">Pieza</th>")
            .append("<th scope=\"col\" style=\"text-align:right;padding:0 0 12px;font-size:14px;color:#6d635d\">Importe</th></tr></thead><tbody>");
        // Usar nombres y precios históricos guardados por Java, nunca el catálogo actual ni datos del navegador.
        for(var d:detalles.findByPedidoIdOrderById(p.getId())) {
            String personalizacion=Stream.of(dato("Grabado",d.getGrabado()),dato("Talla",d.getTalla()),dato("Color",d.getColor()))
                .filter(v->!v.isEmpty()).collect(Collectors.joining(" · "));
            texto.append(d.getCantidad()).append(" × ").append(d.getNombreProducto()).append(" | ").append(dinero(d.getImporte())).append("\n");
            if(!personalizacion.isEmpty()) texto.append("  ").append(personalizacion).append("\n");
            html.append("<tr><td style=\"padding:14px 12px 14px 0;border-bottom:1px solid #eee8e3;overflow-wrap:anywhere\">")
                .append(d.getCantidad()).append(" × ").append(esc(d.getNombreProducto()));
            if(!personalizacion.isEmpty()) html.append("<br><span style=\"font-size:14px;color:#6d635d\">").append(esc(personalizacion)).append("</span>");
            html.append("</td><td style=\"padding:14px 0;text-align:right;vertical-align:top;white-space:nowrap;border-bottom:1px solid #eee8e3\">")
                .append(dinero(d.getImporte())).append("</td></tr>");
        }
        if(p.getDescuento().signum()>0) {
            resumen(texto,html,"Subtotal",dinero(p.getSubtotal()),false);
            resumen(texto,html,"Descuento"+(p.getCodigoCupon()==null?"":" ("+p.getCodigoCupon()+")"),"− "+dinero(p.getDescuento()),false);
        }
        if(p.getCostoEnvio()!=null) resumen(texto,html,"Envío",dinero(p.getCostoEnvio()),false);
        resumen(texto,html,"Total del pedido",dinero(p.getTotal()),true);
        html.append("</tbody></table><p style=\"margin:28px 0 12px\"><strong>Dirección de envío:</strong> ").append(esc(direccion)).append("</p>");
        texto.append("\nDirección de envío: ").append(direccion).append("\n");
        if(p.getReferencia()!=null&&!p.getReferencia().isBlank()) {
            texto.append("Referencia: ").append(p.getReferencia()).append("\n");
            html.append("<p style=\"margin:0 0 12px\">Referencia: ").append(esc(p.getReferencia())).append("</p>");
        }
        String envio=p.getCostoEnvio()==null ? "El envío se cotiza y coordina por separado; no está incluido en este total." : "El costo de envío está incluido en el total.";
        texto.append("\nMedio de pago: ").append(metodo).append("\n").append(estado).append("\n").append(envio)
            .append("\n\nConserva el número del pedido para consultar tu compra.\nGracias por elegir Cielo Joyas.");
        html.append("<p style=\"margin:20px 0\"><strong>Medio de pago:</strong> ").append(esc(metodo)).append("</p>")
            .append("<div style=\"padding:16px;background:#f7f3ef;color:#5d524b;font-size:14px\"><p style=\"margin:0 0 8px\">")
            .append(esc(estado)).append("</p><p style=\"margin:0\">").append(esc(envio)).append("</p></div>")
            .append("<p style=\"margin:24px 0 0;font-size:14px;color:#6d635d\">Conserva el número del pedido para consultar tu compra.<br>Gracias por elegir Cielo Joyas.</p>")
            .append("</td></tr></table></body></html>");
        MimeMessage mensaje=remitente.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(mensaje,true,StandardCharsets.UTF_8.name());
        helper.setValidateAddresses(true);
        helper.setFrom(new InternetAddress(desde,true).getAddress(),"Cielo Joyas");
        helper.setTo(p.getEmail());
        helper.setSubject("Pedido #"+p.getId()+" recibido · Cielo Joyas");
        helper.setSentDate(new Date());
        helper.setText(texto.toString(),html.toString());
        return mensaje;
    }

    private static void resumen(StringBuilder texto,StringBuilder html,String etiqueta,String valor,boolean total) {
        texto.append(etiqueta).append(": ").append(valor).append("\n");
        String peso=total?"font-weight:700;":"";
        html.append("<tr><td style=\"padding:12px 12px 12px 0;").append(peso).append("\">").append(esc(etiqueta))
            .append("</td><td style=\"padding:12px 0;text-align:right;white-space:nowrap;").append(peso).append("\">").append(esc(valor)).append("</td></tr>");
    }
    private static String dato(String etiqueta,String valor) { return valor==null||valor.isBlank()?"":etiqueta+": "+valor; }
    private static String esc(String valor) { return HtmlUtils.htmlEscape(valor==null?"":valor); }
    private static String dinero(BigDecimal valor) { return "S/ "+valor.setScale(2,RoundingMode.HALF_UP).toPlainString(); }
}
