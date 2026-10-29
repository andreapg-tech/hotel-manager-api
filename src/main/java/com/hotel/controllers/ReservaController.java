package com.hotel.controllers;

import com.hotel.dto.ReservaRequest;
import com.hotel.models.Reserva;
import com.hotel.models.Usuario;
import com.hotel.repositories.UsuarioRepository;
import com.hotel.services.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public ReservaController(ReservaService reservaService, UsuarioRepository usuarioRepository) {
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<?> crearReserva(@RequestBody ReservaRequest request, HttpServletRequest httpRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("UNAUTHORIZED: Usuario ilocalizable en la base de datos."));

        // Política de seguridad: Solo CLIENTE (Paridad con ReservaServlet)
        if (!"CLIENTE".equalsIgnoreCase(usuario.getRol())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Acceso denegado: Solo los usuarios con rol CLIENTE pueden realizar reservas desde este portal. Los administradores deben usar el panel de gestión para reservas administrativas.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Validación manual de campos obligatorios
        if (request.getHabitacionId() == null || request.getFechaEntrada() == null || request.getFechaSalida() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cuerpo JSON malformado. Obligatorio: habitacion_id, fecha_entrada, fecha_salida.");
            return ResponseEntity.badRequest().body(error);
        }

        Reserva reserva = reservaService.crearReserva(request, usuario, httpRequest);
        
        long noches = java.time.temporal.ChronoUnit.DAYS.between(reserva.getFechaEntrada(), reserva.getFechaSalida());
        String precioFormateado = reservaService.obtenerPrecioFormateado(httpRequest, reserva.getPrecioTotal());

        return ResponseEntity.status(HttpStatus.CREATED).body(new com.hotel.dto.ReservaResponse(
                "Reserva creada con éxito", 
                noches, 
                precioFormateado
        ));
    }

    @GetMapping
    public ResponseEntity<?> listarMisReservas() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("UNAUTHORIZED: Usuario no encontrado o token revocado."));

        return ResponseEntity.ok(reservaService.listarPorUsuario(usuario.getId()));
    }

    @DeleteMapping
    public ResponseEntity<?> cancelarReserva(@RequestParam Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("UNAUTHORIZED: Token falsificado o usuario revocado."));

        reservaService.cancelarReserva(id, usuario.getId());

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "La reserva ID " + id + " ha sido eliminada con éxito del inventario.");
        return ResponseEntity.ok(response);
    }
}
