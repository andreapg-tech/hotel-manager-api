package com.hotel.controllers;

import com.hotel.models.Usuario;
import com.hotel.repositories.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/portal/empleado")
public class PortalController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PortalController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<?> loginPortal(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Petición incompleta. Se requieren los campos 'email' y 'password'.");
            return ResponseEntity.badRequest().body(error);
        }

        // Validación de formato
        if (!email.trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "El formato del email de empleado no es válido.");
            return ResponseEntity.badRequest().body(error);
        }

        if (password.length() < 8) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Seguridad insuficiente. La contraseña de acceso debe tener al menos 8 caracteres.");
            return ResponseEntity.badRequest().body(error);
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim()).orElse(null);

        // Control de acceso por rol
        if (usuario == null || !"ADMIN".equals(usuario.getRol())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Acceso Denegado. Este portal es exclusivo para empleados con rol administrativo.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Verificación de password
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Acceso Denegado. Las credenciales proporcionadas son incorrectas.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Login exitoso: Sesión Stateful
        HttpSession session = request.getSession(true);
        session.setAttribute("empleado_logueado", usuario.getEmail());
        session.setAttribute("empleado_nombre", usuario.getNombre());
        session.setMaxInactiveInterval(30 * 60);

        Map<String, Object> success = new HashMap<>();
        success.put("mensaje", "Login administrativo exitoso");
        success.put("empleado", usuario.getNombre());
        success.put("session_id", session.getId());
        return ResponseEntity.ok(success);
    }

    @GetMapping
    public ResponseEntity<?> verificarSesion(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("empleado_logueado") == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sesión no activa. Por favor, realice login administrativo.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Map<String, Object> info = new HashMap<>();
        info.put("estado", "Sesión activa");
        info.put("empleado", session.getAttribute("empleado_nombre"));
        info.put("email", session.getAttribute("empleado_logueado"));
        info.put("session_id", session.getId());
        return ResponseEntity.ok(info);
    }

    @DeleteMapping
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Sesión administrativa cerrada correctamente.");
            return ResponseEntity.ok(response);
        }
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "No había ninguna sesión activa para cerrar.");
        return ResponseEntity.ok(response);
    }
}
