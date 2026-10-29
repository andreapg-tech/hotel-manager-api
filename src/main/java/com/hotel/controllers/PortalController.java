package com.hotel.controllers;

import com.hotel.models.Usuario;
import com.hotel.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/portal/empleado")
public class PortalController {

    private final AuthService authService;

    public PortalController(AuthService authService) {
        this.authService = authService;
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

        // Mismo mensaje si el email no existe, si la contraseña es incorrecta o si el usuario
        // no es administrador: así no se puede averiguar qué cuentas existen ni cuáles son de empleados.
        Usuario usuario = authService.buscarConCredenciales(email, password);
        if (usuario == null || !"ADMIN".equals(usuario.getRol())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Acceso denegado. " + AuthService.MENSAJE_CREDENCIALES_INVALIDAS);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Login exitoso: Sesión Stateful
        // Protección contra session fixation: se descarta cualquier sesión previa y se crea
        // una nueva tras autenticarse, así un ID de sesión impuesto por un atacante no sirve.
        HttpSession sesionAnterior = request.getSession(false);
        if (sesionAnterior != null) {
            sesionAnterior.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("empleado_logueado", usuario.getEmail());
        session.setAttribute("empleado_nombre", usuario.getNombre());
        session.setMaxInactiveInterval(30 * 60);

        Map<String, Object> success = new HashMap<>();
        success.put("mensaje", "Login administrativo exitoso");
        success.put("empleado", usuario.getNombre());
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
