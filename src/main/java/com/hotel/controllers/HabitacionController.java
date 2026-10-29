package com.hotel.controllers;

import com.hotel.dto.HabitacionResponseDTO;
import com.hotel.models.Habitacion;
import com.hotel.services.HabitacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    @GetMapping("/habitaciones/disponibles")
    public ResponseEntity<?> buscarDisponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            HttpServletRequest request) {

        // Validaciones manuales para paridad con DisponibilidadServlet
        if (fechaEntrada == null || fechaSalida == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Petición incompleta. Se requieren los parámetros 'fechaEntrada' y 'fechaSalida' en la URL.");
            return ResponseEntity.badRequest().body(error);
        }

        if (fechaEntrada.isAfter(fechaSalida) || fechaEntrada.isEqual(fechaSalida)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "La fecha de entrada debe ser estrictamente anterior a la fecha de salida.");
            return ResponseEntity.badRequest().body(error);
        }

        // Conversión de moneda inversa para el filtro
        BigDecimal precioMaxEuros = habitacionService.convertirAEuros(request, precioMaximo);

        List<Habitacion> disponibles = habitacionService.buscarDisponibles(fechaEntrada, fechaSalida, tipo, precioMaxEuros);

        if (disponibles.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("mensaje", "No se encontraron habitaciones disponibles para los criterios indicados.");
            result.put("fechaEntrada", fechaEntrada.toString());
            result.put("fechaSalida", fechaSalida.toString());
            if (tipo != null) result.put("tipo", tipo);
            if (precioMaximo != null) {
                result.put("precioMaximo", habitacionService.formatearPrecio(request, precioMaximo));
            }
            return ResponseEntity.status(404).body(result);
        }

        // Mapeo a DTO con formato de moneda
        List<HabitacionResponseDTO> dtos = disponibles.stream()
                .map(h -> new HabitacionResponseDTO(
                        h.getId(),
                        h.getNumero(),
                        h.getTipo(),
                        h.getCapacidad(),
                        h.getDisponible(),
                        habitacionService.formatearPrecio(request, h.getPrecioBase())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/admin/habitaciones")
    public ResponseEntity<?> actualizarHabitacion(
            @RequestParam Integer id,
            @RequestBody Habitacion datos,
            HttpServletRequest request) {
        
        habitacionService.actualizar(id, datos);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "La habitación ID " + id + " ha sido actualizada con éxito.");
        return ResponseEntity.ok(response);
    }
}
