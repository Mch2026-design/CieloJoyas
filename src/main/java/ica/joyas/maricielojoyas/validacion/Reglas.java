package ica.joyas.maricielojoyas.validacion;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/** Validaciones compartidas sin dependencias adicionales. */
public final class Reglas {
    private Reglas() {}
    public static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
    public static String texto(Map<String, ?> datos, String campo, int minimo, int maximo) {
        Object valor = datos.get(campo);
        if (valor != null && !(valor instanceof String)) throw error("Revisa el campo " + campo + ".");
        String texto = valor == null ? "" : valor.toString().trim();
        if (texto.length() < minimo || texto.length() > maximo)
            throw error(campo + ": ingresa entre " + minimo + " y " + maximo + " caracteres.");
        return texto;
    }
    public static String email(Map<String, ?> datos) {
        String email = texto(datos, "email", 5, 254).toLowerCase(java.util.Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}")) throw error("Ingresa un correo válido.");
        return email;
    }
    public static long id(Object valor) {
        try {
            long id = new BigDecimal(String.valueOf(valor)).longValueExact();
            if (id <= 0) throw error("El identificador debe ser positivo.");
            return id;
        } catch (NumberFormatException | ArithmeticException e) { throw error("Identificador inválido."); }
    }
    public static int entero(Map<String, ?> datos, String campo, int minimo, int maximo) {
        try {
            int n = new BigDecimal(String.valueOf(datos.get(campo))).intValueExact();
            if (n < minimo || n > maximo) throw error(campo + ": valor fuera del rango permitido.");
            return n;
        } catch (NumberFormatException | ArithmeticException e) { throw error(campo + ": ingresa un número entero."); }
    }
    public static BigDecimal dinero(Object valor) {
        try {
            BigDecimal n = new BigDecimal(String.valueOf(valor)).setScale(2, RoundingMode.UNNECESSARY);
            if (n.signum() < 0 || n.compareTo(new BigDecimal("9999999999.99")) > 0)
                throw error("El importe debe estar entre 0 y 9999999999.99.");
            return n;
        } catch (NumberFormatException | ArithmeticException e) { throw error("Ingresa un importe válido con hasta dos decimales."); }
    }
    public static String opcion(Map<String, ?> datos, String campo, Set<String> opciones) {
        String valor = texto(datos, campo, 1, 30);
        if (!opciones.contains(valor)) throw error("Selecciona una opción válida para " + campo + ".");
        return valor;
    }
    public static boolean bandera(Map<String, ?> datos, String campo) {
        Object valor = datos.get(campo);
        if (valor == null) return false;
        if (!(valor instanceof Boolean)) throw error(campo + ": se esperaba verdadero o falso.");
        return (Boolean) valor;
    }
    public static String url(Map<String, ?> datos, String campo, boolean obligatorio) {
        String valor = texto(datos, campo, obligatorio ? 1 : 0, 2000);
        if (valor.isBlank()) return null;
        try {
            URI uri = URI.create(valor);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                throw error(campo + ": usa una dirección HTTPS completa.");
            return valor;
        } catch (IllegalArgumentException e) { throw error(campo + ": URL inválida."); }
    }
    public static String nulo(String texto) { return texto == null || texto.isBlank() ? null : texto; }
}
