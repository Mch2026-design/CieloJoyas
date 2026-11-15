package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.service.IReporteExcelService;
import ica.joyas.maricielojoyas.validacion.AccesoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas MockMvc de seguridad (401/403/CSRF) y descarga HTTP .xlsx manteniendo AccesoConfig activo. */
@WebMvcTest(ReporteExcelController.class)
@Import({AccesoConfig.class, ErroresApi.class})
class ReporteExcelControllerTests {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IReporteExcelService excelService;

    @MockitoBean
    private AccesoService accesoService;

    @Test
    void sinSesionRetorna401UnauthorizedEnPedidosYResumen() throws Exception {
        when(accesoService.actual(any())).thenReturn(null);

        mockMvc.perform(get("/api/admin/reportes/excel/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/admin/reportes/excel/resumen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(excelService);
    }

    @Test
    void usuarioClienteSinRolAdminRetorna403Forbidden() throws Exception {
        Usuario cliente = new Usuario();
        cliente.setId(20L);
        cliente.setRol("CLIENTE");
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(cliente);

        mockMvc.perform(get("/api/admin/reportes/excel/pedidos").session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/admin/reportes/excel/resumen").session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(excelService);
    }

    @Test
    void adminDescargaExcelPedidosConEncabezadosCorrectos() throws Exception {
        Usuario admin = crearAdmin();
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(admin);
        byte[] contenido = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(excelService.exportarPedidosExcel(30)).thenReturn(contenido);

        mockMvc.perform(get("/api/admin/reportes/excel/pedidos").param("days", "30").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, XLSX_MIME))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cielo-pedidos-30d.xlsx\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(contenido));

        verify(excelService).exportarPedidosExcel(30);
    }

    @Test
    void adminDescargaExcelResumenConEncabezadosCorrectos() throws Exception {
        Usuario admin = crearAdmin();
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(admin);
        byte[] contenido = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(excelService.exportarResumenVentasExcel(7)).thenReturn(contenido);

        mockMvc.perform(get("/api/admin/reportes/excel/resumen").param("days", "7").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, XLSX_MIME))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cielo-reporte-ventas-7d.xlsx\""))
                .andExpect(content().bytes(contenido));

        verify(excelService).exportarResumenVentasExcel(7);
    }

    @Test
    void periodoInvalidoRetorna400BadRequestParaAdmin() throws Exception {
        Usuario admin = crearAdmin();
        MockHttpSession sesion = new MockHttpSession();
        when(accesoService.actual(sesion)).thenReturn(admin);

        mockMvc.perform(get("/api/admin/reportes/excel/pedidos").param("days", "0").session(sesion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/admin/reportes/excel/resumen").param("days", "366").session(sesion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(excelService);
    }

    @Test
    void endpointsDeExportacionSoloPermitenGetYAccesoConfigExigeCsrfEnMutaciones() throws Exception {
        Usuario admin = crearAdmin();
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("csrf", "token-esperado");
        when(accesoService.actual(sesion)).thenReturn(admin);

        mockMvc.perform(post("/api/admin/reportes/excel/pedidos").session(sesion))
                .andExpect(status().isMethodNotAllowed());

        var interceptores = new InterceptorRegistry() {
            List<HandlerInterceptor> registrables() {
                return getInterceptors().stream()
                        .filter(HandlerInterceptor.class::isInstance)
                        .map(HandlerInterceptor.class::cast)
                        .toList();
            }
        };
        new AccesoConfig(accesoService).addInterceptors(interceptores);
        HandlerInterceptor interceptor = interceptores.registrables().get(0);

        MockHttpServletRequest sinCsrf = new MockHttpServletRequest("POST", "/api/admin/reportes/excel/pedidos");
        sinCsrf.setSession(sesion);
        ResponseStatusException errorCsrf = assertThrows(ResponseStatusException.class,
                () -> interceptor.preHandle(sinCsrf, new MockHttpServletResponse(), new Object()));
        assertEquals(HttpStatus.FORBIDDEN, errorCsrf.getStatusCode());

        MockHttpServletRequest conCsrf = new MockHttpServletRequest("POST", "/api/admin/reportes/excel/pedidos");
        conCsrf.setSession(sesion);
        conCsrf.addHeader("X-CSRF-Token", "token-esperado");
        assertTrue(interceptor.preHandle(conCsrf, new MockHttpServletResponse(), new Object()));

        verifyNoInteractions(excelService);
    }

    private static Usuario crearAdmin() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setRol("ADMIN");
        return admin;
    }
}
