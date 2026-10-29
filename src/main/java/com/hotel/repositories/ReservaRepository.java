package com.hotel.repositories;

import com.hotel.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio para la entidad Reserva.
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /**
     * Obtiene las reservas de un usuario ordenadas por fecha de entrada (descendente).
     * Paridad con: ReservaDAO.obtenerReservasPorUsuario
     */
    List<Reserva> findByUsuarioIdOrderByFechaEntradaDesc(Integer usuarioId);

    /**
     * Elimina una reserva si pertenece al usuario indicado (Cancelación segura).
     * Paridad con: ReservaDAO.cancelarReserva
     */
    @Transactional
    void deleteByIdAndUsuarioId(Integer id, Integer usuarioId);

    /**
     * Recupera todas las reservas ordenadas por fecha de entrada (ascendente).
     * Paridad con: ReservaDAO.obtenerTodasParaReporte
     */
    List<Reserva> findAllByOrderByFechaEntradaAsc();

    /**
     * Proyección para las estadísticas de ingresos.
     */
    interface ReservaStats {
        Long getTotalReservasConfirmadas();
        BigDecimal getIngresosTotales();
    }

    /**
     * Calcula métricas globales de ingresos y ocupación.
     * Paridad con: ReservaDAO.obtenerEstadisticas
     */
    @Query("SELECT COUNT(r) AS totalReservasConfirmadas, COALESCE(SUM(r.precioTotal), 0) AS ingresosTotales " +
           "FROM Reserva r WHERE r.estado = 'CONFIRMADA'")
    ReservaStats obtenerEstadisticas();
}
