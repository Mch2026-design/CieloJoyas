package ica.joyas.maricielojoyas.validacion;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CorreoHttpConfigTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CorreoHttpConfig.BrevoSender sender = new CorreoHttpConfig.BrevoSender(builder.build(), "clave-de-prueba");
    private MimeMessage mensaje() throws Exception {
        MimeMessage message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("tienda@example.test", "Cielo Joyas");helper.setTo("cliente@example.test");
        helper.setSubject("Pedido #9 recibido · Cielo Joyas");
        helper.setText("Gracias, Lucía. Total S/ 82.00", "<html><p>Gracias, Lucía. Total S/ 82.00</p></html>");
        return message;
    }
    @Test void enviaPorHttpsElRemitenteDestinatarioYAmbasVersiones() throws Exception {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("api-key", "clave-de-prueba"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sender.email").value("tienda@example.test"))
            .andExpect(jsonPath("$.to[0].email").value("cliente@example.test"))
            .andExpect(jsonPath("$.subject").value("Pedido #9 recibido · Cielo Joyas"))
            .andExpect(jsonPath("$.htmlContent").value("<html><p>Gracias, Lucía. Total S/ 82.00</p></html>"))
            .andExpect(jsonPath("$.textContent").value("Gracias, Lucía. Total S/ 82.00"))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"aceptado-9\"}"));
        sender.send(mensaje());server.verify();
    }
    @Test void rechazoDelProveedorNoAfirmaEnvioNiReintentaAutomaticamente() throws Exception {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("credencial no válida"));
        MimeMessage message = mensaje();
        assertThrows(MailSendException.class, () -> sender.send(message));server.verify();
    }
    @Test void respuestaSinIdentificadorNoSeConsideraEnviada() throws Exception {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));
        MimeMessage message = mensaje();assertThrows(MailSendException.class, () -> sender.send(message));server.verify();
    }
    @Test void sinClaveNoRealizaPeticionNiUsaSmtp() throws Exception {
        var sinClave = new CorreoHttpConfig.BrevoSender(builder.build(), "");
        MimeMessage message = mensaje();assertThrows(MailSendException.class, () -> sinClave.send(message));server.verify();
    }
    @Test void noPermiteCopiasDelPedidoAOtrosDestinatarios() throws Exception {
        MimeMessage message = mensaje();message.setRecipients(Message.RecipientType.BCC, "otro@example.test");
        assertThrows(MailSendException.class, () -> sender.send(message));server.verify();
    }
}
