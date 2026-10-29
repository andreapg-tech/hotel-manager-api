package com.hotel.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preferencias")
public class PreferenceController {

    private static final String NOMBRE_COOKIE = "hotel_moneda";
    private static final String MONEDA_DEFAULT = "EUR";
    private static final int DURACION_DIAS = 30 * 24 * 60 * 60;

    @GetMapping
    public ResponseEntity<?> obtenerPreferencia(HttpServletRequest request) {
        String monedaActual = MONEDA_DEFAULT;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (NOMBRE_COOKIE.equals(c.getName())) {
                    monedaActual = c.getValue();
                    break;
                }
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("moneda", monedaActual);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> guardarPreferencia(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String monedaElegida = body.get("moneda");
        if (monedaElegida == null || monedaElegida.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Campo 'moneda' ausente o vacío en el cuerpo JSON.");
            return ResponseEntity.badRequest().body(error);
        }

        monedaElegida = monedaElegida.trim().toUpperCase();
        List<String> permitidas = List.of("EUR", "USD");
        if (!permitidas.contains(monedaElegida)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Moneda no soportada. Solo se permite EUR o USD");
            return ResponseEntity.badRequest().body(error);
        }

        Cookie cookie = new Cookie(NOMBRE_COOKIE, monedaElegida);
        cookie.setMaxAge(DURACION_DIAS);
        cookie.setPath("/");
        response.addCookie(cookie);

        Map<String, Object> success = new HashMap<>();
        success.put("mensaje", "Preferencia de moneda guardada correctamente.");
        success.put("moneda", monedaElegida);
        success.put("expira_en_dias", 30);
        return ResponseEntity.ok(success);
    }
}
