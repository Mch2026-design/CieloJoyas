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
    @Override public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/tienda/index.html");
        registry.addViewController("/index.html").setViewName("forward:/tienda/index.html");
        registry.addViewController("/catalogo.html").setViewName("forward:/tienda/catalogo.html");
        registry.addViewController("/ofertas.html").setViewName("forward:/tienda/ofertas.html");
        registry.addViewController("/novedades.html").setViewName("forward:/tienda/novedades.html");
        registry.addViewController("/nosotras.html").setViewName("forward:/tienda/nosotras.html");
        registry.addViewController("/personaliza.html").setViewName("forward:/tienda/personaliza.html");
        registry.addViewController("/contacto.html").setViewName("forward:/tienda/contacto.html");
        registry.addViewController("/login.html").setViewName("forward:/acceso/login.html");
        registry.addViewController("/registro.html").setViewName("forward:/acceso/registro.html");
        registry.addViewController("/configurar-admin.html").setViewName("forward:/acceso/configurar-admin.html");
        registry.addViewController("/cuenta.html").setViewName("forward:/cuenta/cuenta.html");
        registry.addViewController("/resenas.html").setViewName("forward:/resenas/resenas.html");
        registry.addViewController("/productos.html").setViewName("forward:/admin/productos.html");
        registry.addViewController("/admin.html").setViewName("forward:/admin/admin.html");
        registry.addViewController("/admin-compras.html").setViewName("forward:/admin/admin-compras.html");
        registry.addViewController("/admin-inventario.html").setViewName("forward:/admin/admin-inventario.html");
        registry.addViewController("/admin-pedidos.html").setViewName("forward:/admin/admin-pedidos.html");
        registry.addViewController("/admin-posventa.html").setViewName("forward:/admin/admin-posventa.html");
        registry.addViewController("/admin-productos.html").setViewName("forward:/admin/admin-productos.html");
        registry.addViewController("/admin-promociones.html").setViewName("forward:/admin/admin-promociones.html");
        registry.addViewController("/admin-reportes.html").setViewName("forward:/admin/admin-reportes.html");
    }
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
                boolean paginaAdmin = (ruta.startsWith("/admin/") && ruta.endsWith(".html"))
                        || ruta.equals("/productos.html") || ruta.equals("/admin.html")
                        || (ruta.startsWith("/admin-") && ruta.endsWith(".html"));
                boolean paginaCuenta = ruta.equals("/cuenta/cuenta.html") || ruta.equals("/cuenta.html");
                if (paginaAdmin || paginaCuenta) {
                    response.setHeader("Cache-Control", "no-store");
                    if (usuario == null) { response.sendRedirect("/acceso/login.html?next=" + ruta); return false; }
                    if (paginaAdmin && !usuario.getRol().equals("ADMIN")) { response.sendRedirect("/tienda/index.html"); return false; }
                    if (paginaCuenta && usuario.getRol().equals("ADMIN")) { response.sendRedirect("/admin/productos.html"); return false; }
                    if (ruta.equals("/admin/admin.html") || ruta.equals("/admin/admin-productos.html")
                            || ruta.equals("/admin.html") || ruta.equals("/admin-productos.html")) {
                        response.sendRedirect("/admin/productos.html"); return false;
                    }
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
        }).addPathPatterns("/api/**", "/assets/certificado-material.*", "/admin/**", "/cuenta/**", "/productos.html", "/admin.html", "/admin-*.html", "/cuenta.html");
    }
}
