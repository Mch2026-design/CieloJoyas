package ica.joyas.maricielojoyas.validacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static ica.joyas.maricielojoyas.validacion.Reglas.error;

/** Guarda una copia normalizada, sin metadatos ni nombres enviados por el cliente. */
@Service
public class FotosResenas {
    private final Path carpeta;
    public FotosResenas(@Value("${cielo.reviews.upload-dir:uploads/reviews}") String carpeta) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }
    public Path ruta(String nombre) {
        if (!nombre.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografía no encontrada.");
        return carpeta.resolve(nombre);
    }
    public String guardar(MultipartFile foto) {
        if (foto == null || foto.isEmpty() || foto.getSize() > 5 * 1024 * 1024)
            throw error("Selecciona una fotografía JPG o PNG de hasta 5 MB.");
        try (var entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(foto.getBytes()))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw error("El archivo debe ser una fotografía JPG o PNG válida.");
            var lector = lectores.next();
            BufferedImage original;
            try {
                if (!Set.of("JPEG", "PNG").contains(lector.getFormatName().toUpperCase(Locale.ROOT)))
                    throw error("Solo se admiten fotografías JPG y PNG.");
                lector.setInput(entrada);
                int w = lector.getWidth(0), h = lector.getHeight(0);
                if (w < 100 || h < 100 || w > 6000 || h > 6000 || (long) w * h > 24000000)
                    throw error("La foto debe medir al menos 100 × 100 píxeles y hasta 6000 por lado (24 megapíxeles).");
                original = lector.read(0);
            } finally { lector.dispose(); }
            double escala = Math.min(1, 1600d / Math.max(original.getWidth(), original.getHeight()));
            var imagen = new BufferedImage((int)(original.getWidth()*escala), (int)(original.getHeight()*escala), BufferedImage.TYPE_INT_RGB);
            var g = imagen.createGraphics();
            try {
                g.setColor(Color.WHITE); g.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(original, 0, 0, imagen.getWidth(), imagen.getHeight(), null);
            } finally { g.dispose(); }
            Files.createDirectories(carpeta);
            String nombre = UUID.randomUUID() + ".jpg";
            if (!ImageIO.write(imagen, "jpg", ruta(nombre).toFile())) throw new IOException("JPEG no disponible");
            return nombre;
        } catch (IOException e) { throw error("No se pudo guardar la fotografía. Revisa el archivo y vuelve a intentarlo."); }
    }
    public void eliminar(String nombre) {
        try { Files.deleteIfExists(ruta(nombre)); } catch (IOException e) { /* Un archivo huérfano nunca se publica. */ }
    }
}
