package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.validacion.AccesoService;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

/** Sesión y comprobación de roles para las páginas y operaciones de la tienda. */
@Configuration
public class AccesoConfig implements WebMvcConfigurer {
    private final AccesoService acceso;
    public AccesoConfig(AccesoService acceso) { this.acceso = acceso; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String ruta = request.getRequestURI();
                response.setHeader("X-Content-Type-Options", "nosniff"); response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("Referrer-Policy", "same-origin");
                Usuario usuario = acceso.actual(request.getSession(false));
                if (ruta.startsWith("/assets/certificado-material.")) {
                    response.setHeader("Cache-Control", "no-store");
                    if (usuario == null || !usuario.getRol().equals("ADMIN"))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El certificado se descarga desde un pedido con pago confirmado.");
                }
                boolean paginaAdmin = ruta.equals("/productos.html") || ruta.equals("/admin.html") || (ruta.startsWith("/admin-") && ruta.endsWith(".html"));
                if (paginaAdmin || ruta.equals("/cuenta.html")) {
                    response.setHeader("Cache-Control", "no-store");
                    if (usuario == null) { response.sendRedirect("/login.html?next=" + ruta.substring(1)); return false; }
                    if (paginaAdmin && !usuario.getRol().equals("ADMIN")) { response.sendRedirect("/index.html"); return false; }
                    if (ruta.equals("/cuenta.html") && usuario.getRol().equals("ADMIN")) { response.sendRedirect("/productos.html"); return false; }
                    if (ruta.equals("/admin.html") || ruta.equals("/admin-productos.html")) { response.sendRedirect("/productos.html"); return false; }
                }
                if (ruta.startsWith("/api/")) {
                    response.setHeader("Cache-Control", "no-store");
                    if (ruta.startsWith("/api/admin/") || ruta.startsWith("/api/account/")) {
                        if (usuario == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
                        if (ruta.startsWith("/api/admin/") && !usuario.getRol().equals("ADMIN"))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta sección corresponde a la administración de la tienda.");
                    }
                    if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
                        HttpSession sesion = request.getSession(false);
                        String esperado = sesion == null ? null : (String) sesion.getAttribute("csrf");
                        if (esperado == null || !esperado.equals(request.getHeader("X-CSRF-Token")))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actualiza la página y vuelve a intentarlo.");
                    }
                }
                return true;
            }
        }).addPathPatterns("/api/**", "/assets/certificado-material.*", "/productos.html", "/admin.html", "/admin-*.html", "/cuenta.html");
    }
}
