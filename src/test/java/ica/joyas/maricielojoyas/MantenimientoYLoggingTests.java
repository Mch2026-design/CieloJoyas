package ica.joyas.maricielojoyas;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica la configuración segura de Logback y las herramientas preventivas de mantenimiento/. */
class MantenimientoYLoggingTests {

    @Test
    void logbackSpringXmlEsValidoYProtegeInformacionSensible() throws Exception {
        ClassPathResource resource = new ClassPathResource("logback-spring.xml");
        assertTrue(resource.exists());

        Document doc;
        try (var stream = resource.getInputStream()) {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            assertEquals("configuration", doc.getDocumentElement().getNodeName());
        }

        String xml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("name=\"CONSOLA\""));
        assertTrue(xml.contains("name=\"ARCHIVO\""));
        assertTrue(xml.contains("%replace(%msg)"));
        assertTrue(xml.contains("name=\"org.hibernate.SQL\" level=\"WARN\""));
        assertTrue(xml.contains("name=\"org.hibernate.orm.jdbc.bind\" level=\"WARN\""));

        String rawPattern = XPathFactory.newInstance().newXPath()
                .evaluate("//property[@name='LOG_PATTERN']/@value", doc)
                .replace("${APP_NAME}", "maricielojoyas");
        LoggerContext ctx = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setContext(ctx);
        layout.setPattern(rawPattern);
        layout.start();

        assertTrue(layout.isStarted(), "El patrón de Logback debe compilar correctamente");
        boolean tieneErroresStatus = ctx.getStatusManager().getCopyOfStatusList().stream()
                .anyMatch(s -> s.getLevel() >= Status.ERROR);
        assertFalse(tieneErroresStatus, "Logback no debe registrar errores al compilar LOG_PATTERN");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("ica.joyas.maricielojoyas.Prueba");
        event.setLevel(Level.INFO);
        event.setMessage("Login intento password=MiClaveSecreta123 token=tok_abc999}");
        event.setTimeStamp(System.currentTimeMillis());

        String salida = layout.doLayout(event);
        assertFalse(salida.contains("MiClaveSecreta123"), "No debe exponerse password en logs");
        assertFalse(salida.contains("tok_abc999"), "No debe exponerse token en logs");
        assertTrue(salida.contains("password=***"));
        assertTrue(salida.contains("token=***"));
    }

    @Test
    void herramientasDeMantenimientoExistenYSqlEsNoDestructivo() throws Exception {
        Path carpeta = Path.of("mantenimiento");
        assertTrue(Files.isDirectory(carpeta));

        for (String archivo : List.of(
                "README.md",
                "jconsole-monitoreo.ps1",
                "backup-bd.ps1",
                "backup-bd.sh",
                "mantenimiento-preventivo.sql")) {
            assertTrue(Files.isRegularFile(carpeta.resolve(archivo)), "Falta archivo: " + archivo);
        }

        List<String> lineasActivas = Files.readAllLines(carpeta.resolve("mantenimiento-preventivo.sql"), StandardCharsets.UTF_8)
                .stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("--"))
                .map(l -> l.toUpperCase(Locale.ROOT))
                .toList();

        assertFalse(lineasActivas.isEmpty());
        for (String linea : lineasActivas) {
            assertFalse(linea.startsWith("DROP "), "Sentencia destructiva no permitida: " + linea);
            assertFalse(linea.startsWith("TRUNCATE "), "Sentencia destructiva no permitida: " + linea);
            assertFalse(linea.startsWith("DELETE "), "Sentencia destructiva no permitida: " + linea);
            assertFalse(linea.startsWith("UPDATE "), "Sentencia destructiva no permitida: " + linea);
        }
    }
}
