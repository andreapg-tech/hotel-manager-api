package com.hotel.services;

import com.hotel.models.Usuario;
import com.hotel.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND: El usuario asociado al token no existe."));
    }

    @Transactional
    public void cambiarPassword(String email, Map<String, String> passwords) {
        String actual = passwords.get("password_actual");
        String nueva = passwords.get("password_nuevo");

        if (actual == null || nueva == null) {
            throw new RuntimeException("BAD_REQUEST: Estructura JSON inválida. Faltan los campos password_actual y/o password_nuevo.");
        }

        if (actual.equals(nueva)) {
            throw new RuntimeException("BAD_REQUEST: Rechazado: La nueva contraseña no puede ser idéntica a la que ya estás usando actualmente.");
        }

        if (nueva.length() < 8) {
            throw new RuntimeException("BAD_REQUEST: Contraseña demasiado corta. Por seguridad, debe tener al menos 8 caracteres.");
        }

        Usuario usuario = obtenerPorEmail(email);

        if (!passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new RuntimeException("FORBIDDEN: Prohibido: La contraseña actual facilitada es incorrecta.");
        }

        usuario.setPassword(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);
    }
}
