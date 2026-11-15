package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.validacion.AccesoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de matriz de control de acceso (A01 Broken Access Control),
 * validación CSRF y cabeceras de seguridad HTTP en AccesoConfig.
 */
class ControlAccesoSeguridadTests {

    private AccesoService accesoService;
    private HandlerInterceptor interceptor;

    @BeforeEach
    void setUp() {
        accesoService = mock(AccesoService.class);
        AccesoConfig config = new AccesoConfig(accesoService);
        final HandlerInterceptor[] capturado = new HandlerInterceptor[1];
        InterceptorRegistry registry = new InterceptorRegistry() {
            @Override
            public InterceptorRegistration addInterceptor(HandlerInterceptor hi) {
                capturado[0] = hi;
                return super.addInterceptor(hi);
            }
        };
        config.addInterceptors(registry);
        interceptor = capturado[0];
        assertNotNull(interceptor);
    }

    private Usuario crearUsuario(long id, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        return u;
    }

    @Test
    void anonimoEnRutasAdminYCuentaRetorna401Unauthorized() {
        when(accesoService.actual(any())).thenReturn(null);

        for (String ruta : new String[]{"/api/admin/productos", "/api/admin/pedidos", "/api/account/perfil"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", ruta);
            MockHttpServletResponse res = new MockHttpServletResponse();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> interceptor.preHandle(req, res, new Object()));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode(), "Ruta esperada 401: " + ruta);
        }
    }

    @Test
    void clienteEnRutasAdminYCertificadoRetorna403Forbidden() {
        Usuario cliente = crearUsuario(15L, "CLIENTE");
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(cliente);

        for (String ruta : new String[]{
                "/api/admin/productos",
                "/api/admin/pedidos",
                "/api/admin/reportes",
                "/assets/certificado-material.pdf"
        }) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", ruta);
            req.setSession(sesion);
            MockHttpServletResponse res = new MockHttpServletResponse();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> interceptor.preHandle(req, res, new Object()));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode(), "Ruta esperada 403 para CLIENTE: " + ruta);
        }
    }

    @Test
    void clienteEnPaginaAdminRedirigeAIndexYVerificaCabecerasSeguridad() throws Exception {
        Usuario cliente = crearUsuario(16L, "CLIENTE");
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(cliente);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/admin-reportes.html");
        req.setSession(sesion);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean permitido = interceptor.preHandle(req, res, new Object());
        assertFalse(permitido);
        assertEquals("/tienda/index.html", res.getRedirectedUrl());
        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", res.getHeader("X-Frame-Options"));
        assertEquals("same-origin", res.getHeader("Referrer-Policy"));
        assertEquals("no-store", res.getHeader("Cache-Control"));
    }

    @Test
    void mutacionApiSinTokenCsrfValidoRetorna403Forbidden() {
        Usuario cliente = crearUsuario(17L, "CLIENTE");
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("csrf", "token-real-123");
        when(accesoService.actual(sesion)).thenReturn(cliente);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/account/perfil");
        req.setSession(sesion);
        req.addHeader("X-CSRF-Token", "token-invalido");
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interceptor.preHandle(req, res, new Object()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void adminConSesionYCsrfValidoAccedeARutasProtegidas() throws Exception {
        Usuario admin = crearUsuario(1L, "ADMIN");
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("csrf", "csrf-admin-ok");
        when(accesoService.actual(sesion)).thenReturn(admin);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/admin/productos");
        req.setSession(sesion);
        req.addHeader("X-CSRF-Token", "csrf-admin-ok");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(req, res, new Object()));
        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", res.getHeader("X-Frame-Options"));
    }
}
