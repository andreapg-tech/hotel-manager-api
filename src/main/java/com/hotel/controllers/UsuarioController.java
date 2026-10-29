package com.hotel.controllers;

import com.hotel.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> passwords) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        usuarioService.cambiarPassword(email, passwords);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Tu contraseña maestra ha sido actualizada de forma segura.");
        return ResponseEntity.ok(response);
    }
}
