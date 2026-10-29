package com.hotel.controllers;

import com.hotel.dto.ReservaRequest;
import com.hotel.models.Reserva;
import com.hotel.models.Usuario;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UsuarioRepository;
import com.hotel.services.ReservaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public AdminController(ReservaRepository reservaRepository, ReservaService reservaService, UsuarioRepository usuarioRepository) {
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas() {
        ReservaRepository.ReservaStats stats = reservaRepository.obtenerEstadisticas();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalReservasConfirmadas", stats.getTotalReservasConfirmadas());
        response.put("ingresosTotales", stats.getIngresosTotales().toString() + " €");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservas")
    public ResponseEntity<?> listarReservas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Paginación de Spring (0-indexed)
        int springPage = page - 1;
        if (springPage < 0) springPage = 0;
        size = Math.min(size, 100);

        Page<Reserva> pagedReservas = reservaRepository.findAll(PageRequest.of(springPage, size));
        reservaService.poblarDatosHabitacion(pagedReservas.getContent());

        Map<String, Object> response = new HashMap<>();
        response.put("data", pagedReservas.getContent());
        response.put("current_page", page);
        response.put("total_pages", pagedReservas.getTotalPages());
        response.put("total_elements", pagedReservas.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservas")
    public ResponseEntity<?> eliminarReserva(@RequestParam Integer id) {
        if (!reservaRepository.existsById(id)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se encontró ninguna reserva con ID " + id + ".");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        reservaRepository.deleteById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Reserva eliminada correctamente por el administrador");
        response.put("reserva_id", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservas")
    public ResponseEntity<?> crearReservaAdmin(@RequestBody Reserva datos, @RequestHeader("Authorization") String auth) {
        // En Spring Boot el objeto 'datos' ya viene mapeado.
        // Simulamos la lógica de AdminReservaServlet
        
        if (datos.getUsuarioId() == null || datos.getHabitacionId() == null ||
            datos.getFechaEntrada() == null || datos.getFechaSalida() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "JSON incompleto. Se requiere: usuario_id, habitacion_id, fecha_entrada, fecha_salida.");
            return ResponseEntity.badRequest().body(error);
        }

        Usuario beneficiario = usuarioRepository.findById(datos.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("NOT_FOUND: El usuario beneficiario indicado no existe en el sistema."));

        if (!"CLIENTE".equalsIgnoreCase(beneficiario.getRol())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Prohibido: Desde este panel solo se pueden realizar reservas para usuarios con rol CLIENTE. No se permiten reservas para administradores o personal interno.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Usamos el servicio pero adaptado a la creación administrativa
        ReservaRequest request = new ReservaRequest();
        request.setHabitacionId(datos.getHabitacionId());
        request.setFechaEntrada(datos.getFechaEntrada());
        request.setFechaSalida(datos.getFechaSalida());

        Reserva reserva = reservaService.crearReserva(request, beneficiario, null); 

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Reserva formalizada por administración");
        response.put("precio_final", reserva.getPrecioTotal().toString() + " €");
        response.put("cliente_id", reserva.getUsuarioId());
        response.put("reserva_detalles", reserva);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reportes/reservas")
    public void exportarReservasCSV(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_reservas.csv\"");

        java.io.PrintWriter out = response.getWriter();
        out.println("ID_Reserva,Numero_Habitacion,Usuario_ID,Fecha_Entrada,Fecha_Salida,Estado,Precio_Total (EUR)");

        java.util.List<Reserva> historial = reservaRepository.findAllByOrderByFechaEntradaAsc();
        reservaService.poblarDatosHabitacion(historial);

        for (Reserva r : historial) {
            String habNumero = (r.getHabitacionNumero() != null) ? r.getHabitacionNumero() : String.valueOf(r.getHabitacionId());
            out.println(r.getId() + "," +
                        habNumero + "," +
                        r.getUsuarioId() + "," +
                        r.getFechaEntrada() + "," +
                        r.getFechaSalida() + "," +
                        r.getEstado() + "," +
                        r.getPrecioTotal() + " €");
        }
    }
}
