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

    // Mensaje único para cualquier fallo de login: no revela si el email existe.
    public static final String MENSAJE_CREDENCIALES_INVALIDAS = "Credenciales inválidas.";

    // Hash de referencia para comparar cuando el usuario no existe. Así el login
    // tarda lo mismo exista o no el email, y el tiempo de respuesta tampoco lo delata.
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.hashFicticio = passwordEncoder.encode("hash-ficticio-para-igualar-tiempos");
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

        Usuario usuario = buscarConCredenciales(email, request.getPassword());
        if (usuario == null) {
            throw new RuntimeException("UNAUTHORIZED: " + MENSAJE_CREDENCIALES_INVALIDAS);
        }
        return usuario;
    }

    /**
     * Devuelve el usuario si el email y la contraseña son correctos, o null en caso contrario.
     * Siempre ejecuta una comparación BCrypt para que el tiempo de respuesta sea el mismo
     * tanto si el email existe como si no.
     */
    public Usuario buscarConCredenciales(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        String hash = (usuario != null) ? usuario.getPassword() : hashFicticio;
        boolean passwordCorrecta = passwordEncoder.matches(password, hash);
        return (usuario != null && passwordCorrecta) ? usuario : null;
    }
}
