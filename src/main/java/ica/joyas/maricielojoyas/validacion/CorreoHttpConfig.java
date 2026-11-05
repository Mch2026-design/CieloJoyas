package ica.joyas.maricielojoyas.validacion;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Render Free bloquea SMTP; Brevo usa HTTPS sin cambiar el contenido del aviso. */
@Configuration(proxyBeanMethods = false)
public class CorreoHttpConfig {
    @Bean
    @ConditionalOnProperty(name = "cielo.mail.transport", havingValue = "brevo")
    JavaMailSender correoBrevo(@Value("${cielo.mail.brevo-api-key:}") String apiKey) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        return new BrevoSender(RestClient.builder().requestFactory(factory)
            .baseUrl("https://api.brevo.com/v3").build(), apiKey);
    }

    static final class BrevoSender extends JavaMailSenderImpl {
        private final RestClient client;
        private final String apiKey;
        BrevoSender(RestClient client, String apiKey) { this.client = client; this.apiKey = apiKey.trim(); }

        @Override protected void doSend(MimeMessage[] messages, Object[] originals) {
            if (apiKey.isBlank()) throw new MailSendException("Falta BREVO_API_KEY en el servidor.");
            for (MimeMessage message : messages) {
                try {
                    message.saveChanges();
                    var sender = (InternetAddress) message.getFrom()[0];
                    var recipients = message.getRecipients(Message.RecipientType.TO);
                    if (recipients == null || recipients.length != 1
                        || message.getRecipients(Message.RecipientType.CC) != null
                        || message.getRecipients(Message.RecipientType.BCC) != null)
                        throw new MailSendException("El aviso requiere un único destinatario.");
                    var recipient = (InternetAddress) recipients[0];
                    String html = content(message, "text/html"), text = content(message, "text/plain");
                    if (html.isBlank()) throw new MailSendException("El aviso requiere contenido HTML.");
                    var body = Map.of("sender", Map.of("email", sender.getAddress(), "name", "Cielo Joyas"),
                        "to", List.of(Map.of("email", recipient.getAddress())), "subject", message.getSubject(),
                        "htmlContent", html, "textContent", text);
                    // No reintentar automáticamente: una respuesta perdida podría duplicar el correo.
                    var response = client.post().uri("/smtp/email").header("api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .body(body).retrieve().toEntity(Map.class);
                    Object messageId = response.getBody() == null ? null : response.getBody().get("messageId");
                    if (response.getStatusCode().value() != 201 || !(messageId instanceof String id) || id.isBlank())
                        throw new MailSendException("Brevo no confirmó la aceptación del mensaje.");
                } catch (MessagingException | IOException | RestClientException error) {
                    // No incluir clave, destinatario ni respuesta del proveedor en el mensaje de error.
                    throw new MailSendException("No se pudo entregar el aviso al proveedor HTTPS.", error);
                }
            }
        }

        private static String content(Part part, String type) throws MessagingException, IOException {
            if (part.isMimeType(type)) return (String) part.getContent();
            if (part.getContent() instanceof Multipart multipart)
                for (int i = 0; i < multipart.getCount(); i++) {
                    String value = content(multipart.getBodyPart(i), type);
                    if (!value.isBlank()) return value;
                }
            return "";
        }
    }
}
