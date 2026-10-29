package com.hotel.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.RegisterRequest;
import com.hotel.models.Habitacion;
import com.hotel.models.Usuario;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UsuarioRepository;
import com.hotel.utils.TokensUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TokensUtil tokensUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private HabitacionRepository habitacionRepository;

    @MockitoBean
    private ReservaRepository reservaRepository;

    private String tokenValidoCliente;
    private String tokenValidoAdmin;

    @BeforeEach
    public void setup() {
        // Generar tokens reales firmados para las peticiones de prueba
        tokenValidoCliente = tokensUtil.generateToken("cliente@test.com", "CLIENTE");
        tokenValidoAdmin = tokensUtil.generateToken("admin@test.com", "ADMIN");
    }

    @Test
    @DisplayName("1. Ruta Pública: Buscar Habitaciones Disponibles devuelve 200 OK")
    public void testBuscarHabitacionesDisponibles() throws Exception {
        Habitacion h = new Habitacion();
        h.setId(1);
        h.setNumero("101");
        h.setTipo("DOBLE");
        h.setPrecioBase(new java.math.BigDecimal("50.00"));

        when(habitacionRepository.findAll()).thenReturn(Collections.singletonList(h));
        // El servicio suele filtrar, pero aquí mockeamos lo necesario
        when(habitacionRepository.findDisponibles(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(h));

        mockMvc.perform(get("/api/habitaciones/disponibles")
                .param("fechaEntrada", "2026-10-01")
                .param("fechaSalida", "2026-10-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("101"));
    }

    @Test
    @DisplayName("2. Login Exitoso: Devuelve Token JWT")
    public void testLoginExitoso() throws Exception {
        Usuario mockUser = new Usuario();
        mockUser.setEmail("test@hotel.com");
        // El password que se guarda en DB es el hash. Encriptamos "password123"
        mockUser.setPassword(passwordEncoder.encode("password123"));
        mockUser.setRol("CLIENTE");

        when(usuarioRepository.findByEmailIgnoreCase("test@hotel.com")).thenReturn(Optional.of(mockUser));

        String jsonLogin = "{\"email\": \"test@hotel.com\", \"password\": \"password123\"}";

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("3. Falta de Token: Devuelve 401 Unauthorized")
    public void testFaltaToken() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("Acceso denegado. Se requiere un token Bearer en el header Authorization."));
    }

    @Test
    @DisplayName("4. Token Inválido: Devuelve 401 Unauthorized")
    public void testTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/reservas")
                .header("Authorization", "Bearer tokenInventadoQueVaAFallar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token de acceso corrupto o mal formado."));
    }

    @Test
    @DisplayName("5. Acceso Denegado por Rol: CLIENTE intenta acceder a /admin")
    public void testAccesoDenegadoPorRol() throws Exception {
        mockMvc.perform(get("/api/admin/estadisticas")
                .header("Authorization", "Bearer " + tokenValidoCliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permisos suficientes para acceder a este recurso."));
    }

    @Test
    @DisplayName("6. Ruta Inexistente: Devuelve 404 Not Found JSON (Prioridad MVC sobre Security)")
    public void testRutaInexistente() throws Exception {
        // Usamos un prefijo que esté en 'permitAll' para que pase el filtro de
        // seguridad
        // pero que no exista en los controladores, disparando el 404 real.
        mockMvc.perform(get("/api/habitaciones/esta/ruta/no/existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La ruta solicitada no existe o no soporta este método HTTP."));
    }

    @Test
    @DisplayName("7. Método No Soportado: Enviar POST a una ruta GET")
    public void testMetodoNoSoportado() throws Exception {
        // Hacemos POST a estadísticas enviando Token Admin válido
        mockMvc.perform(post("/api/admin/estadisticas")
                .header("Authorization", "Bearer " + tokenValidoAdmin))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("El método HTTP POST no está soportado en esta ruta."));
    }

    @Test
    @DisplayName("8. Validación Manual: Error 400 en Registro sin Email")
    public void testValidacionDatosRegistro() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Prueba");
        req.setPassword("secreta123");
        // Dejamos el email nulo a propósito

        mockMvc.perform(post("/api/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Faltan datos obligatorios para el registro: nombre, email o password no pueden estar en blanco."));
    }
}
