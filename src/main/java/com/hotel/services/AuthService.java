package com.hotel.services;

import com.hotel.dto.LoginRequest;
import com.hotel.dto.RegisterRequest;
import com.hotel.models.Usuario;
import com.hotel.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario aplicando las validaciones originales.
     */
    public void registrar(RegisterRequest request) {
        // Normalización
        String email = request.getEmail().trim().toLowerCase();

        // Validaciones de formato
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new RuntimeException("BAD_REQUEST: Formato de email inválido.");
        }
        if (request.getPassword().length() < 8) {
            throw new RuntimeException("BAD_REQUEST: Seguridad insuficiente: La contraseña debe tener al menos 8 caracteres.");
        }

        // Duplicados
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            // Usamos un marcador para que el ExceptionHandler sepa que es un 409
            throw new RuntimeException("CONFLICT: El correo electrónico ya está registrado en el sistema.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol("CLIENTE"); // Rol estrictamente forzado a CLIENTE por seguridad en registro público

        usuarioRepository.save(usuario);
    }

    /**
     * Verifica las credenciales de login.
     */
    public Usuario verificarCredenciales(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(
                        () -> new RuntimeException("UNAUTHORIZED: Credenciales inválidas. Usuario no encontrado."));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("UNAUTHORIZED: Credenciales inválidas. Contraseña incorrecta.");
        }

        return usuario;
    }
}
