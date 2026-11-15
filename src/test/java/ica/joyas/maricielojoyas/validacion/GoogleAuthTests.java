package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Usuario;
import ica.joyas.maricielojoyas.repositorio.ClienteRepository;
import ica.joyas.maricielojoyas.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthTests {

    @Test
    void googleAuthServiceHabilitadoSoloConUrlYAnonKey() {
        GoogleAuthService sinKey = new GoogleAuthService("https://demo.supabase.co/", "");
        assertFalse(sinKey.habilitado());
        assertEquals("https://demo.supabase.co", sinKey.getSupabaseUrl());

        GoogleAuthService completo = new GoogleAuthService("https://demo.supabase.co/", "anon-key-123");
        assertTrue(completo.habilitado());
    }

    @Test
    void verificarTokenSinAnonKeyDevuelve503() {
        GoogleAuthService servicio = new GoogleAuthService("https://demo.supabase.co", "");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.verificarToken("token-ejemplo"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void verificarTokenVacioDevuelve401() {
        GoogleAuthService servicio = new GoogleAuthService("https://demo.supabase.co", "anon-key-123");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.verificarToken("   "));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void autenticarORegistrarGoogleCreaNuevoClienteYReutilizaExistente() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AccesoService acceso = new AccesoService(usuarios, clientes, jdbc);

        when(usuarios.findByEmailIgnoreCase("nuevo@gmail.com")).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = acceso.autenticarORegistrarGoogle("nuevo@gmail.com", "Lucia", "Gomez");
        assertEquals("CLIENTE", creado.getRol());
        assertEquals("nuevo@gmail.com", creado.getEmail());
        assertEquals("Lucia Gomez", creado.getNombre());
        verify(usuarios).saveAndFlush(any(Usuario.class));
        verify(clientes).registrarSiFalta("Lucia", "Gomez", "nuevo@gmail.com", null, false);

        Usuario existente = new Usuario();
        existente.setEmail("viejo@gmail.com");
        existente.setNombre("Ana Ruiz");
        existente.setRol("CLIENTE");
        existente.setActivo(true);
        when(usuarios.findByEmailIgnoreCase("viejo@gmail.com")).thenReturn(Optional.of(existente));

        Usuario reutilizado = acceso.autenticarORegistrarGoogle("viejo@gmail.com", "Ana", "Ruiz");
        assertSame(existente, reutilizado);
        verify(clientes, never()).registrarSiFalta("Ana", "Ruiz", "viejo@gmail.com", null, false);
    }

    @Test
    void autenticarORegistrarGoogleRechazaUsuarioInactivoCon403() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AccesoService acceso = new AccesoService(usuarios, clientes, jdbc);

        Usuario inactivo = new Usuario();
        inactivo.setEmail("bloqueado@gmail.com");
        inactivo.setActivo(false);
        when(usuarios.findByEmailIgnoreCase("bloqueado@gmail.com")).thenReturn(Optional.of(inactivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> acceso.autenticarORegistrarGoogle("bloqueado@gmail.com", "Luis", "Diaz"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void restablecerPasswordValidaComplejidadYActualizaHash() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AccesoService acceso = new AccesoService(usuarios, clientes, jdbc);

        assertThrows(ResponseStatusException.class, () -> acceso.validarPasswordSegura("Corta1!"));
        assertThrows(ResponseStatusException.class, () -> acceso.validarPasswordSegura("SinNumeroEspecial"));
        assertThrows(ResponseStatusException.class, () -> acceso.validarPasswordSegura("sinmayuscula1!"));
        assertThrows(ResponseStatusException.class, () -> acceso.validarPasswordSegura("SinEspecial1234"));
        assertDoesNotThrow(() -> acceso.validarPasswordSegura("CieloJoyas2026!"));

        Usuario existente = new Usuario();
        existente.setEmail("cliente@gmail.com");
        existente.setActivo(true);
        existente.setRol("CLIENTE");
        when(usuarios.findByEmailIgnoreCase("cliente@gmail.com")).thenReturn(Optional.of(existente));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = acceso.restablecerPassword("cliente@gmail.com", "Ana", "Lopez",
                "CieloJoyas2026!", "CieloJoyas2026!");
        assertTrue(ClaveAcceso.coincide("CieloJoyas2026!", actualizado.getPasswordHash()));
    }
}
