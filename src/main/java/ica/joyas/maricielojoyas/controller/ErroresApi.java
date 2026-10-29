package ica.joyas.maricielojoyas.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ErroresApi {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> negocio(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "Revisa los datos." : e.getReason()));
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> fotoGrande(Exception e) {
        return ResponseEntity.status(413).body(Map.of("error", "La fotografía debe pesar como máximo 5 MB."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("error", "Ya existe ese registro o hay datos relacionados. Revisa los valores y actualiza la página."));
    }
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<?> concurrencia(ConcurrencyFailureException e) {
        return ResponseEntity.status(409).body(Map.of("error", "Otro proceso actualizó este registro. Vuelve a intentarlo."));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<?> formato(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "El formato de los datos es incorrecto. Revisa el formulario."));
    }
}
