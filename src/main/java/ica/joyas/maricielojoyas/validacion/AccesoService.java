package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.repositorio.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static ica.joyas.maricielojoyas.validacion.Reglas.*;

@Service
public class AccesoService {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final JdbcTemplate jdbc;
    private final String hashDeComparacion = ClaveAcceso.guardar(UUID.randomUUID().toString());
    public AccesoService(UsuarioRepository usuarios, ClienteRepository clientes, JdbcTemplate jdbc) {
        this.usuarios = usuarios; this.clientes = clientes; this.jdbc = jdbc;
    }
    public Usuario actual(HttpSession sesion) {
        if (sesion == null || !(sesion.getAttribute("usuarioId") instanceof Long id)) return null;
        return usuarios.findById(id).filter(Usuario::getActivo).orElse(null);
    }
    public Usuario requerido(HttpSession sesion) {
        Usuario u = actual(sesion);
        if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        return u;
    }
    public boolean esLocal(HttpServletRequest request) {
        return Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(request.getRemoteAddr());
    }
    public String token(HttpSession sesion) {
        if (sesion.getAttribute("csrf") == null) sesion.setAttribute("csrf", UUID.randomUUID().toString());
        return (String) sesion.getAttribute("csrf");
    }
    public Map<String, Object> vista(Usuario u) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", u.getId()); datos.put("name", u.getNombre()); datos.put("email", u.getEmail()); datos.put("role", u.getRol());
        clientes.findByEmailIgnoreCase(u.getEmail()).ifPresent(c -> {
            datos.put("firstName", c.getNombres()); datos.put("lastName", c.getApellidos()); datos.put("phone", c.getTelefono());
        });
        return datos;
    }
    public Map<String, Object> sesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(); Usuario u = actual(sesion);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("csrf", token(sesion)); datos.put("user", u == null ? null : vista(u));
        datos.put("needsAdmin", esLocal(request) && !usuarios.existsByRol("ADMIN"));
        return datos;
    }
    public Map<String, Object> entrar(Usuario usuario, HttpServletRequest request) {
        HttpSession sesion = request.getSession(); request.changeSessionId();
        sesion.setAttribute("usuarioId", usuario.getId()); sesion.setAttribute("csrf", UUID.randomUUID().toString());
        sesion.removeAttribute("intentos"); sesion.removeAttribute("bloqueadoHasta");
        return sesion(request);
    }
    public Usuario autenticar(Map<String, Object> datos, HttpSession sesion) {
        Long hasta = (Long) sesion.getAttribute("bloqueadoHasta");
        if (hasta != null && hasta > System.currentTimeMillis())
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espera un minuto antes de volver a intentarlo.");
        String correo = email(datos), clave = password(datos);
        Usuario usuario = usuarios.findByEmailIgnoreCaseAndActivoTrue(correo).orElse(null);
        boolean coincide = ClaveAcceso.coincide(clave, usuario == null ? hashDeComparacion : usuario.getPasswordHash());
        if (usuario == null || !coincide) {
            int intentos = sesion.getAttribute("intentos") == null ? 1 : (Integer) sesion.getAttribute("intentos") + 1;
            sesion.setAttribute("intentos", intentos);
            if (intentos >= 5) { sesion.setAttribute("bloqueadoHasta", System.currentTimeMillis() + 60000L); sesion.setAttribute("intentos", 0); }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El correo o la contraseña no coinciden.");
        }
        return usuario;
    }
    private String password(Map<String, Object> datos) {
        if (!(datos.get("password") instanceof String clave) || clave.isEmpty() || clave.length() > 128)
            throw error("Ingresa una contraseña de hasta 128 caracteres.");
        return clave;
    }
    @Transactional
    public Usuario registrar(Map<String, Object> datos, boolean administrador) {
        if (administrador) {
            // Serializa únicamente la configuración del primer administrador.
            jdbc.execute("SELECT pg_advisory_xact_lock(72104963)");
            if (usuarios.existsByRol("ADMIN")) throw new ResponseStatusException(HttpStatus.CONFLICT, "La tienda ya tiene un administrador. Inicia sesión.");
        }
        String correo = email(datos);
        if (usuarios.findByEmailIgnoreCase(correo).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con este correo. Inicia sesión.");
        String nombres = texto(datos, "firstName", 2, 50), apellidos = texto(datos, "lastName", 2, 49);
        String clave = password(datos);
        if (clave.length() < 8) throw error("La contraseña debe tener al menos 8 caracteres.");
        if ((administrador || datos.containsKey("passwordConfirmation")) && !clave.equals(datos.get("passwordConfirmation"))) throw error("Las contraseñas deben coincidir.");
        String telefono = texto(datos, "phone", 0, 9);
        if (!telefono.isBlank() && !telefono.matches("9[0-9]{8}")) throw error("El celular debe tener 9 dígitos y empezar por 9.");
        Usuario usuario = new Usuario(); usuario.setNombre(nombres + " " + apellidos); usuario.setEmail(correo);
        usuario.setPasswordHash(ClaveAcceso.guardar(clave)); usuario.setRol(administrador ? "ADMIN" : "CLIENTE");
        usuarios.saveAndFlush(usuario);
        if (!administrador) clientes.registrarSiFalta(nombres, apellidos, correo, nulo(telefono), false);
        return usuario;
    }
}
