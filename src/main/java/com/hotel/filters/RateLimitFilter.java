package com.hotel.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final ConcurrentHashMap<String, Long> tiemposDePeticion = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> contadoresPeticion = new ConcurrentHashMap<>();

    private static final long VENTANA_TIEMPO_MS = 60000;
    private static final int MAX_PETICIONES = 5;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"/api/login".equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipCliente = request.getRemoteAddr();
        long ahora = System.currentTimeMillis();

        tiemposDePeticion.putIfAbsent(ipCliente, ahora);
        long tiempoUltimaVentana = tiemposDePeticion.get(ipCliente);

        if (ahora - tiempoUltimaVentana > VENTANA_TIEMPO_MS) {
            tiemposDePeticion.put(ipCliente, ahora);
            contadoresPeticion.put(ipCliente, 0);
        }

        int peticionesActuales = contadoresPeticion.merge(ipCliente, 1, Integer::sum);

        if (peticionesActuales > MAX_PETICIONES) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print("{\"error\": \"Límite de peticiones excedido. Por seguridad, solo se permiten " 
                    + MAX_PETICIONES + " intentos de inicio de sesión por minuto. Inténtelo más tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
