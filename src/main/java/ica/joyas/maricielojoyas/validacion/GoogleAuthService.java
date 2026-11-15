package ica.joyas.maricielojoyas.validacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** Verificación de tokens de acceso de Google emitidos mediante Supabase Auth. */
@Service
public class GoogleAuthService {

    public record PerfilGoogle(String email, String nombres, String apellidos) {}

    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final RestClient restClient;

    public GoogleAuthService(
            @Value("${cielo.supabase.url:}") String supabaseUrl,
            @Value("${cielo.supabase.anon-key:}") String supabaseAnonKey) {
        this.supabaseUrl = normalizarUrl(supabaseUrl);
        this.supabaseAnonKey = supabaseAnonKey == null ? "" : supabaseAnonKey.trim();
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(8000);
        this.restClient = RestClient.builder().requestFactory(rf).build();
    }

    private static String normalizarUrl(String url) {
        if (url == null) return "";
        return url.trim().replaceAll("/+$", "");
    }

    public boolean habilitado() {
        return !supabaseUrl.isBlank() && !supabaseAnonKey.isBlank();
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public PerfilGoogle verificarToken(String accessToken) {
        if (!habilitado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El acceso con Google aún no está configurado en el servidor (falta SUPABASE_ANON_KEY).");
        }
        if (accessToken == null || accessToken.isBlank() || accessToken.length() > 4096) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No se recibió una sesión válida de Google. Intenta nuevamente.");
        }
        Map<String, Object> respuesta;
        try {
            respuesta = restClient.get()
                    .uri(supabaseUrl + "/auth/v1/user")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No se pudo validar tu acceso con Google. Vuelve a intentarlo.");
        }
        if (respuesta == null || !(respuesta.get("email") instanceof String rawEmail) || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google no devolvió un correo electrónico válido.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String nombres = "";
        String apellidos = "";
        if (respuesta.get("user_metadata") instanceof Map<?, ?> meta) {
            Object given = meta.get("given_name");
            Object family = meta.get("family_name");
            Object full = meta.get("full_name") != null ? meta.get("full_name") : meta.get("name");
            if (given instanceof String g && !g.isBlank()) nombres = g.trim();
            if (family instanceof String f && !f.isBlank()) apellidos = f.trim();
            if ((nombres.isBlank() || apellidos.isBlank()) && full instanceof String fn && !fn.isBlank()) {
                String[] partes = fn.trim().split("\\s+", 2);
                if (nombres.isBlank()) nombres = partes[0];
                if (apellidos.isBlank()) apellidos = partes.length > 1 ? partes[1] : "Cliente";
            }
        }
        if (nombres.isBlank()) nombres = email.substring(0, email.indexOf('@'));
        if (apellidos.isBlank()) apellidos = "Google";
        if (nombres.length() > 50) nombres = nombres.substring(0, 50);
        if (apellidos.length() > 49) apellidos = apellidos.substring(0, 49);
        return new PerfilGoogle(email, nombres, apellidos);
    }

    public void solicitarRecuperacion(String email, String redirectTo) {
        if (!habilitado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recuperación de contraseña por Supabase aún no está configurada (falta SUPABASE_ANON_KEY).");
        }
        String uri = supabaseUrl + "/auth/v1/recover";
        if (redirectTo != null && !redirectTo.isBlank()) {
            uri += "?redirect_to=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8);
        }
        try {
            restClient.post()
                    .uri(uri)
                    .header("apikey", supabaseAnonKey)
                    .header("Content-Type", "application/json")
                    .body(Map.of("email", email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo solicitar el correo de recuperación en Supabase. Intenta nuevamente en unos minutos.");
        }
    }

    public void actualizarPasswordEnSupabase(String accessToken, String nuevaPassword) {
        if (!habilitado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar SUPABASE_ANON_KEY en el servidor.");
        }
        try {
            restClient.put()
                    .uri(supabaseUrl + "/auth/v1/user")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .body(Map.of("password", nuevaPassword))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El enlace de recuperación expiró o no es válido. Solicita uno nuevo.");
        }
    }
}
