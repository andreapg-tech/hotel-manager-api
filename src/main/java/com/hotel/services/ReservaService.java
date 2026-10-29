package com.hotel.services;

import com.hotel.dto.ReservaRequest;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Usuario;
import com.hotel.repositories.ReservaRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HabitacionService habitacionService;

    public ReservaService(ReservaRepository reservaRepository, HabitacionService habitacionService) {
        this.reservaRepository = reservaRepository;
        this.habitacionService = habitacionService;
    }

    @Transactional
    public Reserva crearReserva(ReservaRequest request, Usuario usuario, HttpServletRequest httpRequest) {
        // 1. Validaciones temporales
        if (!request.getFechaEntrada().isBefore(request.getFechaSalida())) {
            throw new RuntimeException(
                    "BAD_REQUEST: Incoherencia temporal. La fecha_entrada tiene que ser anterior a fecha_salida.");
        }

        // 2. Obtener habitación
        Habitacion habitacion = habitacionService.obtenerPorId(request.getHabitacionId());

        // 3. Calcular precio (Lógica migrada de PrecioUtil)
        BigDecimal precioTotal = calcularPrecioFinal(habitacion, request.getFechaEntrada(), request.getFechaSalida());

        // 4. Persistir
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(usuario.getId());
        reserva.setHabitacionId(habitacion.getId());
        reserva.setFechaEntrada(request.getFechaEntrada());
        reserva.setFechaSalida(request.getFechaSalida());
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado("CONFIRMADA");

        reserva.setHabitacionNumero(habitacion.getNumero());
        reserva.setHabitacionTipo(habitacion.getTipo());

        return reservaRepository.save(reserva);
    }

    public String obtenerPrecioFormateado(HttpServletRequest request, BigDecimal precio) {
        return formatearPrecio(request, precio);
    }

    public List<Reserva> listarPorUsuario(Integer usuarioId) {
        List<Reserva> reservas = reservaRepository.findByUsuarioIdOrderByFechaEntradaDesc(usuarioId);
        poblarDatosHabitacion(reservas);
        return reservas;
    }

    public void poblarDatosHabitacion(List<Reserva> reservas) {
        if (reservas == null || reservas.isEmpty()) return;
        List<Habitacion> todas = habitacionService.obtenerTodas();
        java.util.Map<Integer, Habitacion> mapa = todas.stream()
                .collect(java.util.stream.Collectors.toMap(Habitacion::getId, h -> h));
        for (Reserva r : reservas) {
            Habitacion h = mapa.get(r.getHabitacionId());
            if (h != null) {
                r.setHabitacionNumero(h.getNumero());
                r.setHabitacionTipo(h.getTipo());
            } else {
                r.setHabitacionNumero(String.valueOf(r.getHabitacionId()));
            }
        }
    }

    @Transactional
    public void cancelarReserva(Integer reservaId, Integer usuarioId) {
        List<Reserva> usuarioReservas = listarPorUsuario(usuarioId);
        Reserva reserva = usuarioReservas.stream()
                .filter(r -> r.getId().equals(reservaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "FORBIDDEN: Operación denegada. La reserva no existe o usted no es el titular legítimo."));

        if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
            throw new RuntimeException("BAD_REQUEST: Esta reserva ya se encuentra cancelada.");
        }

        if (reserva.getFechaEntrada().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "BAD_REQUEST: No es posible cancelar una estancia cuya fecha de entrada ya ha pasado.");
        }

        reservaRepository.deleteByIdAndUsuarioId(reservaId, usuarioId);
    }

    // --- LÓGICA DE NEGOCIO MIGRADA DE UTILS ---

    private BigDecimal calcularPrecioFinal(Habitacion habitacion, LocalDate entrada, LocalDate salida) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal base = habitacion.getPrecioBase();
        LocalDate actual = entrada;

        while (actual.isBefore(salida)) {
            BigDecimal costoDia = base;
            if (actual.getDayOfWeek() == DayOfWeek.SATURDAY || actual.getDayOfWeek() == DayOfWeek.SUNDAY) {
                costoDia = costoDia.multiply(new BigDecimal("1.15"));
            }
            if (actual.getMonth() == Month.JULY || actual.getMonth() == Month.AUGUST) {
                costoDia = costoDia.multiply(new BigDecimal("1.30"));
            }
            total = total.add(costoDia);
            actual = actual.plusDays(1);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatearPrecio(HttpServletRequest request, BigDecimal precio) {
        String moneda = "EUR";
        BigDecimal cambio = new BigDecimal("1.15");
        String simbolo = "€";

        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("hotel_moneda".equals(c.getName())) {
                        moneda = c.getValue().toUpperCase();
                        break;
                    }
                }
            }
        }

        if ("USD".equals(moneda)) {
            precio = precio.multiply(cambio).setScale(2, RoundingMode.HALF_UP);
            simbolo = "$";
        }

        return precio.toPlainString() + " " + simbolo;
    }
}
