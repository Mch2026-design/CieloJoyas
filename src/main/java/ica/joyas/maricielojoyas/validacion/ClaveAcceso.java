package ica.joyas.maricielojoyas.validacion;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Contraseñas con sal aleatoria mediante las APIs de Java. */
public final class ClaveAcceso {
    private static final int ITERACIONES = 210000;
    private ClaveAcceso() {}
    private static byte[] derivar(String texto, byte[] sal, int vueltas) {
        PBEKeySpec spec = new PBEKeySpec(texto.toCharArray(), sal, vueltas, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("No se pudo procesar la contraseña.", e); }
        finally { spec.clearPassword(); }
    }
    public static String guardar(String texto) {
        byte[] sal = new byte[16]; new SecureRandom().nextBytes(sal);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$"
            + Base64.getEncoder().encodeToString(derivar(texto, sal, ITERACIONES));
    }
    public static boolean coincide(String texto, String hash) {
        try {
            String[] partes = hash.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
            int vueltas = Integer.parseInt(partes[1]);
            if (vueltas < 10000 || vueltas > 1000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(partes[3]),
                derivar(texto, Base64.getDecoder().decode(partes[2]), vueltas));
        } catch (IllegalArgumentException e) { return false; }
    }
}
