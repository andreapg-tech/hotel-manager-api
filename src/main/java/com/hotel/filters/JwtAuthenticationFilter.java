package com.hotel.filters;

import com.hotel.utils.TokensUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokensUtil tokensUtil;

    public JwtAuthenticationFilter(TokensUtil tokensUtil) {
        this.tokensUtil = tokensUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (tokensUtil.validateToken(token)) {
                String email = tokensUtil.extractEmail(token);
                String rol = tokensUtil.extractClaim(token, "rol");

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())
                );

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        email, null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (ExpiredJwtException e) {
            responderErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "El token ha caducado. Vuelve a hacer login para renovarlo.");
            return;
        } catch (SignatureException e) {
            responderErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "El token no es válido o ha sido falsificado (firma incorrecta).");
            return;
        } catch (JwtException e) {
            responderErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Token de acceso corrupto o mal formado.");
            return;
        } catch (Exception e) {
            responderErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Error de autenticación: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderErrorJSON(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print("{\"error\": \"" + mensaje + "\"}");
        out.flush();
    }
}
