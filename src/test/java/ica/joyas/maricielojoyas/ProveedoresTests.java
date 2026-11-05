package ica.joyas.maricielojoyas;

import ica.joyas.maricielojoyas.controller.AppController;
import ica.joyas.maricielojoyas.modelo.Proveedor;
import ica.joyas.maricielojoyas.repositorio.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProveedoresTests {
    private final ProveedorRepository repository = mock(ProveedorRepository.class);
    private AppController controller() {
        AppController controller = new AppController();
        ReflectionTestUtils.setField(controller, "proveedores", repository);
        when(repository.save(any(Proveedor.class))).thenAnswer(i -> i.getArgument(0));
        return controller;
    }
    @Test void registraProveedorEnApartadoElegido() {
        Proveedor p = controller().guardarProveedor(Map.of("name", "Proveedor de acero", "supplyCategory", "acero", "contact", "acero@example.test"));
        assertEquals("acero", p.getCategoriaInsumo());
        assertEquals("acero@example.test", p.getContacto());
        assertTrue(p.getActivo());
    }
    @Test void rechazaApartadoDesconocidoSinGuardar() {
        AppController controller = controller();
        assertThrows(ResponseStatusException.class, () -> controller.guardarProveedor(Map.of("name", "Proveedor", "supplyCategory", "desconocido")));
        verify(repository, never()).save(any());
    }
    @Test void editarConservaIdentidadYPermiteDesactivar() {
        Proveedor previo = new Proveedor(); previo.setId(12L); previo.setCategoriaInsumo("acero");
        when(repository.findById(12L)).thenReturn(Optional.of(previo));
        Proveedor actualizado = controller().guardarProveedor(Map.of("id", "12", "name", "Nuevo nombre", "supplyCategory", "gemas", "active", false));
        assertSame(previo, actualizado); assertEquals(12L, actualizado.getId());
        assertEquals("gemas", actualizado.getCategoriaInsumo()); assertFalse(actualizado.getActivo());
    }
    @Test void clienteAnteriorNoBorraElApartadoAlEditar() {
        Proveedor previo = new Proveedor(); previo.setId(12L); previo.setCategoriaInsumo("perlas");
        when(repository.findById(12L)).thenReturn(Optional.of(previo));
        assertEquals("perlas", controller().guardarProveedor(Map.of("id", "12", "name", "Proveedor actualizado")).getCategoriaInsumo());
    }
    @Test void registraImagenHttpsYPermiteQuitarla() {
        Proveedor previo = new Proveedor(); previo.setId(12L);
        when(repository.findById(12L)).thenReturn(Optional.of(previo));
        AppController api = controller();
        assertEquals("https://imagenes.example.test/logo.png", api.guardarProveedor(Map.of("id", "12", "name", "Proveedor", "imageUrl", "https://imagenes.example.test/logo.png")).getImagenUrl());
        assertNull(api.guardarProveedor(Map.of("id", "12", "name", "Proveedor", "imageUrl", "")).getImagenUrl());
    }
    @Test void rechazaImagenesConProtocolosInsegurosSinGuardar() {
        AppController api = controller();
        for (String value : java.util.List.of("http://ejemplo.test/foto.png", "javascript:alert(1)", "data:image/png;base64,AAAA", "no-es-url", "https://ejemplo.test/" + "a".repeat(2000)))
            assertThrows(ResponseStatusException.class, () -> api.guardarProveedor(Map.of("name", "Proveedor", "imageUrl", value)));
        verify(repository, never()).save(any());
    }
    @Test void unaEdicionAnteriorConservaLaImagenGuardada() {
        Proveedor previo = new Proveedor(); previo.setId(12L); previo.setImagenUrl("https://imagenes.example.test/logo.png");
        when(repository.findById(12L)).thenReturn(Optional.of(previo));
        assertEquals(previo.getImagenUrl(), controller().guardarProveedor(Map.of("id", "12", "name", "Proveedor actualizado")).getImagenUrl());
    }

}
