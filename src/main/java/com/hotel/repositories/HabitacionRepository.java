package com.hotel.repositories;

import com.hotel.models.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la entidad Habitacion.
 */
@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    /**
     * Busca una habitación por su número único.
     */
    java.util.Optional<Habitacion> findByNumero(String numero);

    /**
     * Busca habitaciones disponibles en un rango de fechas, con filtros opcionales de tipo y precio.
     * 
     * Implementa la lógica de HabitacionDAO.obtenerDisponibles:
     * - h.id NOT IN subcláusula de reservas CONFIRMADAS que solapan las fechas.
     * - h.disponible = true.
     * - Filtros dinámicos manejados mediante lógica condicional en la query (compatible con H2/MySQL).
     *
     * @param fechaEntrada Inicio estancia
     * @param fechaSalida Fin estancia
     * @param tipo Tipo de habitación (opcional, pasar NULL para ignorar)
     * @param precioMax Precio máximo (opcional, pasar NULL para ignorar)
     */
    @Query("SELECT h FROM Habitacion h WHERE NOT EXISTS (" +
           "  SELECT 1 FROM Reserva r " +
           "  WHERE r.habitacionId = h.id " +
           "  AND r.estado = 'CONFIRMADA' " +
           "  AND r.fechaEntrada < :fechaSalida " +
           "  AND r.fechaSalida > :fechaEntrada" +
           ") AND h.disponible = true " +
           "AND (:tipo IS NULL OR :tipo = '' OR h.tipo = :tipo) " +
           "AND (:precioMax IS NULL OR h.precioBase <= :precioMax)")
    List<Habitacion> findDisponibles(
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("tipo") String tipo,
            @Param("precioMax") BigDecimal precioMax
    );
}
