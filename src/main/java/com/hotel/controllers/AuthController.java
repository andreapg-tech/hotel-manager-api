package com.hotel.controllers;

import com.hotel.dto.AuthResponse;
import com.hotel.dto.LoginRequest;
import com.hotel.dto.RegisterRequest;
import com.hotel.models.Usuario;
import com.hotel.services.AuthService;
import com.hotel.utils.TokensUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final TokensUtil tokensUtil;

    public AuthController(AuthService authService, TokensUtil tokensUtil) {
        this.authService = authService;
        this.tokensUtil = tokensUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Validación manual de campos para paridad con el original
        if (request.getEmail() == null || request.getEmail().isBlank() || 
            request.getPassword() == null || request.getPassword().isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Formato inválido. Se requiere email y password verificados no vacíos.");
            return ResponseEntity.badRequest().body(error);
        }

        Usuario usuario = authService.verificarCredenciales(request);
        String token = tokensUtil.generateToken(usuario.getEmail(), usuario.getRol());
        
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegisterRequest request) {
        // Validación manual de campos para paridad con el original
        if (request.getNombre() == null || request.getNombre().isBlank() ||
            request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Faltan datos obligatorios para el registro: nombre, email o password no pueden estar en blanco.");
            return ResponseEntity.badRequest().body(error);
        }

        authService.registrar(request);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Usuario registrado correctamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
