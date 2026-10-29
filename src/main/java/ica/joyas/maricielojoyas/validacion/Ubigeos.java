package ica.joyas.maricielojoyas.validacion;

import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class Ubigeos {
    private final Map<String, String[]> localidades = new HashMap<>();
    public Ubigeos() throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("ubigeos.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines().forEach(linea -> {
                String[] partes = linea.split("\t", -1);
                localidades.put(partes[0], partes);
            });
        }
    }
    public String[] buscar(String codigo) {
        String[] fila = localidades.get(codigo);
        if (fila == null) throw Reglas.error("Selecciona un departamento, provincia y distrito válidos.");
        return fila.clone();
    }
}
