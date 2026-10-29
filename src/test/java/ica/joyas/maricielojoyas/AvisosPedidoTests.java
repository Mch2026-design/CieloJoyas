package ica.joyas.maricielojoyas;

import ica.joyas.maricielojoyas.modelo.*;
import ica.joyas.maricielojoyas.repositorio.*;
import ica.joyas.maricielojoyas.validacion.AvisosPedido;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvisosPedidoTests {
    private final PedidoRepository pedidos=mock(PedidoRepository.class);
    private final DetallePedidoRepository detalles=mock(DetallePedidoRepository.class);
    private final PagoRepository pagos=mock(PagoRepository.class);
    private final JavaMailSender smtp=mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> remitentes=mock(ObjectProvider.class);

    private Pedido preparar() {
        Pedido p=new Pedido();p.setId(9L);p.setNombres("Lucía");p.setEmail("cliente@example.test");
        p.setDireccion("Av. Pruebas 123");p.setDistrito("Ica");p.setProvincia("Ica");p.setDepartamento("Ica");
        p.setSubtotal(new BigDecimal("82.00"));p.setTotal(new BigDecimal("82.00"));
        Pago pago=new Pago();pago.setMetodo("yape");pago.setEstado("pendiente");
        DetallePedido d=new DetallePedido();d.setNombreProducto("Tobillera Estrellas");d.setCantidad(2);
        d.setPrecioUnitario(new BigDecimal("41.00"));d.setImporte(new BigDecimal("82.00"));
        d.setGrabado("María");d.setTalla("18 cm");d.setColor("Dorado");
        when(pedidos.bloquearPorId(9L)).thenReturn(Optional.of(p));
        when(detalles.findByPedidoIdOrderById(9L)).thenReturn(List.of(d));
        when(pagos.findByPedidoId(9L)).thenReturn(Optional.of(pago));
        when(remitentes.getIfAvailable()).thenReturn(smtp);
        when(smtp.createMimeMessage()).thenAnswer(invocation->new MimeMessage(Session.getInstance(new Properties())));
        return p;
    }
    private AvisosPedido servicio(boolean activo,String desde) {return new AvisosPedido(pedidos,detalles,pagos,remitentes,activo,desde);}
    private MimeMessage enviado() throws Exception {
        var captor=ArgumentCaptor.forClass(MimeMessage.class);verify(smtp).send(captor.capture());
        captor.getValue().saveChanges();return captor.getValue();
    }
    private String parte(Part part,String tipo) throws Exception {
        if(part.isMimeType(tipo))return (String)part.getContent();
        if(part.getContent() instanceof Multipart multiple)
            for(int i=0;i<multiple.getCount();i++){String texto=parte(multiple.getBodyPart(i),tipo);if(texto!=null)return texto;}
        return null;
    }
    @Test void sinConfiguracionNoAfirmaEnvio() {
        Pedido p=preparar();assertEquals("not_configured",servicio(false,"").enviar(9L));
        assertNull(p.getCorreoEnviadoEn());verifyNoInteractions(smtp);
    }
    @Test void sinRemitenteNoAfirmaEnvio() {
        preparar();when(remitentes.getIfAvailable()).thenReturn(null);
        assertEquals("not_configured",servicio(true,"tienda@example.test").enviar(9L));verifyNoInteractions(smtp);
    }
    @Test void unFalloSmtpConservaElPedidoYPermiteReintentar() {
        Pedido p=preparar();doThrow(new MailSendException("Fallo de prueba")).when(smtp).send(any(MimeMessage.class));
        assertEquals("failed",servicio(true,"tienda@example.test").enviar(9L));
        assertNull(p.getCorreoEnviadoEn());verify(pedidos,never()).save(any());
        doNothing().when(smtp).send(any(MimeMessage.class));
        assertEquals("sent",servicio(true,"tienda@example.test").enviar(9L));assertNotNull(p.getCorreoEnviadoEn());
    }
    @Test void enviaHtmlYTextoConComprasHistoricasAlCorreoDelPedido() throws Exception {
        Pedido p=preparar();assertEquals("sent",servicio(true,"Naia <tienda@example.test>").enviar(9L));
        MimeMessage m=enviado();assertNotNull(p.getCorreoEnviadoEn());verify(pedidos).save(p);
        assertEquals("Pedido #9 recibido · Cielo Joyas",m.getSubject());
        assertEquals("Cielo Joyas",((InternetAddress)m.getFrom()[0]).getPersonal());
        assertEquals("cliente@example.test",((InternetAddress)m.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertNull(m.getRecipients(Message.RecipientType.CC));assertNull(m.getRecipients(Message.RecipientType.BCC));
        for(String contenido:List.of(parte(m,"text/plain"),org.springframework.web.util.HtmlUtils.htmlUnescape(parte(m,"text/html")))) {
            assertTrue(contenido.contains("2 × Tobillera Estrellas"));assertTrue(contenido.contains("S/ 82.00"));
            assertTrue(contenido.contains("María"));assertTrue(contenido.contains("18 cm"));assertTrue(contenido.contains("Dorado"));
            assertTrue(contenido.contains("Av. Pruebas 123, Ica, Ica, Ica"));assertTrue(contenido.contains("Yape"));
            assertTrue(contenido.contains("pago está pendiente"));assertTrue(contenido.contains("no está incluido"));
            assertFalse(contenido.contains("NAIA"));assertFalse(contenido.contains("demostración"));
        }
    }
    @Test void escapaLosDatosPersonalesEnElHtml() throws Exception {
        Pedido p=preparar();p.setNombres("Lucía <img src=x>");p.setDireccion("Av. A & B <script>alert(1)</script>");
        var d=detalles.findByPedidoIdOrderById(9L).getFirst();d.setNombreProducto("Anillo <b>especial</b>");d.setGrabado("A&B");
        assertEquals("sent",servicio(true,"tienda@example.test").enviar(9L));String html=parte(enviado(),"text/html");
        assertTrue(html.contains("&lt;script&gt;"));assertTrue(html.contains("A&amp;B"));
        assertTrue(html.contains("Anillo &lt;b&gt;especial&lt;/b&gt;"));assertFalse(html.contains("<img src=x>"));
        assertFalse(html.contains("<script>"));
    }
    @Test void muestraDescuentoYEnvioSinAlterarLosImportes() throws Exception {
        Pedido p=preparar();p.setCodigoCupon("CIELO10");p.setDescuento(new BigDecimal("8.20"));
        p.setCostoEnvio(new BigDecimal("5.00"));p.setTotal(new BigDecimal("78.80"));
        servicio(true,"tienda@example.test").enviar(9L);String text=parte(enviado(),"text/plain");
        assertTrue(text.contains("Subtotal: S/ 82.00"));assertTrue(text.contains("Descuento (CIELO10): − S/ 8.20"));
        assertTrue(text.contains("Envío: S/ 5.00"));assertTrue(text.contains("Total del pedido: S/ 78.80"));
        assertTrue(text.contains("El costo de envío está incluido"));
    }
    @Test void unReintentoTardioNoAnunciaPagoPendienteSiYaFueConfirmado() throws Exception {
        Pedido p=preparar();p.setEstado("pagado");pagos.findByPedidoId(9L).orElseThrow().setEstado("confirmado");
        servicio(true,"tienda@example.test").enviar(9L);String text=parte(enviado(),"text/plain");
        assertTrue(text.contains("Tu pago está confirmado"));assertFalse(text.contains("pago está pendiente"));
    }
    @Test void unPedidoCanceladoNoSolicitaPago() throws Exception {
        preparar().setEstado("cancelado");servicio(true,"tienda@example.test").enviar(9L);
        assertTrue(parte(enviado(),"text/plain").contains("Pedido cancelado. No realices un nuevo pago."));
    }
    @Test void reintentarNoVuelveAEnviarUnCorreoYaAceptado() {
        preparar().setCorreoEnviadoEn(OffsetDateTime.now());
        assertEquals("sent",servicio(true,"tienda@example.test").enviar(9L));verifyNoInteractions(smtp);
    }
    @Test void unRemitenteInvalidoNoAfirmaEnvio() {
        Pedido p=preparar();assertEquals("failed",servicio(true,"correo incorrecto").enviar(9L));
        assertNull(p.getCorreoEnviadoEn());verify(smtp,never()).send(any(MimeMessage.class));
    }
}
