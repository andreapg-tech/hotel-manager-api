package com.hotel.repositories;

import com.hotel.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 * Proporciona acceso a datos y métodos de búsqueda personalizados.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su email ignorando mayúsculas/minúsculas.
     * 
     * @param email Email a buscar.
     * @return Optional con el usuario si se encuentra.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
