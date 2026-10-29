package com.hotel.services;

import com.hotel.models.Habitacion;
import com.hotel.repositories.HabitacionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;

    public HabitacionService(HabitacionRepository habitacionRepository) {
        this.habitacionRepository = habitacionRepository;
    }

    public List<Habitacion> obtenerTodas() {
        return habitacionRepository.findAll();
    }

    public Habitacion obtenerPorId(Integer id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND: La habitación solicitada no consta en los registros de inventario."));
    }

    public List<Habitacion> buscarDisponibles(LocalDate entrada, LocalDate salida, String tipo, BigDecimal precioMax) {
        // Validación de coherencia temporal (del original DisponibilidadServlet)
        if (entrada != null && salida != null && !entrada.isBefore(salida)) {
            throw new RuntimeException("BAD_REQUEST: La fecha de entrada debe ser estrictamente anterior a la fecha de salida.");
        }

        // Validación estricta de tipo (Paridad con DisponibilidadServlet líneas 75-83)
        if (tipo != null && !tipo.isBlank()) {
            String tipoNorm = tipo.trim().toUpperCase();
            if (!Arrays.asList("INDIVIDUAL", "DOBLE", "SUITE").contains(tipoNorm)) {
                throw new RuntimeException("BAD_REQUEST: Criterio de búsqueda inválido. Los tipos permitidos son: INDIVIDUAL, DOBLE o SUITE.");
            }
        }

        return habitacionRepository.findDisponibles(entrada, salida, tipo, precioMax);
    }

    @Transactional
    public Habitacion actualizar(Integer id, Habitacion datosNuevos) {
        Habitacion habitacion = obtenerPorId(id);

        // 1. Número de habitación
        if (datosNuevos.getNumero() != null && !datosNuevos.getNumero().isBlank()) {
            if (!datosNuevos.getNumero().matches("-?\\d+")) {
                throw new RuntimeException("BAD_REQUEST: El número de habitación debe ser un valor numérico (puede ser negativo para plantas subterráneas).");
            }
            habitacion.setNumero(datosNuevos.getNumero());
        }

        // 2. Tipo y Capacidad (Lógica Smart)
        if (datosNuevos.getTipo() != null && !datosNuevos.getTipo().isBlank()) {
            String tipo = datosNuevos.getTipo().trim().toUpperCase();
            if (!Arrays.asList("INDIVIDUAL", "DOBLE", "SUITE").contains(tipo)) {
                throw new RuntimeException("BAD_REQUEST: Tipo de habitación no válido. Use: individual, doble o suite.");
            }
            habitacion.setTipo(tipo);

            // Inferencia de capacidad si no viene en el JSON
            if (datosNuevos.getCapacidad() == null) {
                switch (tipo) {
                    case "INDIVIDUAL": habitacion.setCapacidad(1); break;
                    case "DOBLE": habitacion.setCapacidad(2); break;
                    case "SUITE": habitacion.setCapacidad(4); break;
                }
            }
        }

        // 3. Validación de Capacidad explícita
        if (datosNuevos.getCapacidad() != null) {
            if (!Arrays.asList(1, 2, 4).contains(datosNuevos.getCapacidad())) {
                throw new RuntimeException("BAD_REQUEST: La capacidad solo puede ser 1, 2 o 4 personas.");
            }
            habitacion.setCapacidad(datosNuevos.getCapacidad());
        }

        // 4. Coherencia Tipo-Capacidad
        validarCoherencia(habitacion.getTipo(), habitacion.getCapacidad());

        // 5. Precio
        if (datosNuevos.getPrecioBase() != null) {
            if (datosNuevos.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("BAD_REQUEST: El precio base debe ser un valor positivo estrictamente mayor que cero.");
            }
            habitacion.setPrecioBase(datosNuevos.getPrecioBase());
        }

        // 6. Disponibilidad
        if (datosNuevos.getDisponible() != null) {
            habitacion.setDisponible(datosNuevos.getDisponible());
        }

        return habitacionRepository.save(habitacion);
    }

    private void validarCoherencia(String tipo, Integer capacidad) {
        boolean coherente = false;
        if ("INDIVIDUAL".equals(tipo) && capacidad == 1) coherente = true;
        else if ("DOBLE".equals(tipo) && capacidad == 2) coherente = true;
        else if ("SUITE".equals(tipo) && capacidad == 4) coherente = true;

        if (!coherente) {
            throw new RuntimeException("BAD_REQUEST: Incoherencia lógica: El tipo " + tipo + " debe tener capacidad " +
                    ("INDIVIDUAL".equals(tipo) ? "1" : ("DOBLE".equals(tipo) ? "2" : "4")) + ".");
        }
    }

    // --- LÓGICA DE MONEDA MIGRADA ---

    public String formatearPrecio(HttpServletRequest request, BigDecimal precio) {
        String moneda = "EUR";
        BigDecimal cambio = new BigDecimal("1.15");
        String simbolo = "€";

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("hotel_moneda".equals(c.getName())) {
                    moneda = c.getValue().toUpperCase();
                    break;
                }
            }
        }

        if ("USD".equals(moneda)) {
            precio = precio.multiply(cambio).setScale(2, java.math.RoundingMode.HALF_UP);
            simbolo = "$";
        }

        return precio.toPlainString() + " " + simbolo;
    }

    public BigDecimal convertirAEuros(HttpServletRequest request, BigDecimal precioEnMoneda) {
        if (precioEnMoneda == null) return null;

        Cookie[] cookies = request.getCookies();
        String moneda = "EUR";
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("hotel_moneda".equals(c.getName())) {
                    moneda = c.getValue().toUpperCase();
                    break;
                }
            }
        }

        if ("USD".equals(moneda)) {
            return precioEnMoneda.divide(new BigDecimal("1.15"), java.math.MathContext.DECIMAL64)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return precioEnMoneda;
    }
}
