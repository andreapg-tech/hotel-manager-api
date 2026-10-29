package com.hotel.listeners;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class AppLifecycleHandler {

    private static final Logger LOGGER = Logger.getLogger(AppLifecycleHandler.class.getName());

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println();
        System.out.println("╔═════════════════════════════════════════════════════╗");
        System.out.println("║       Hotel Pergon API Iniciada Correctamente       ║");
        System.out.println("╠═════════════════════════════════════════════════════╣");
        System.out.println("║  Aplicación : " + padRight("hotel-manager-springboot", 38) + "║");
        System.out.println("║  Estado     : " + padRight("OPERATIVA (Spring Boot 3)", 38) + "║");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        System.out.println();

        LOGGER.info("=== Hotel Pergon API Iniciada Correctamente (Spring Boot) ===");
    }

    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void onShutdown() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║               Hotel Pergon API Detenida              ║");
        System.out.println("║         Servidor apagado limpiamente. Hasta pronto.  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        LOGGER.info("=== Hotel Pergon API Detenida ===");
    }

    private String padRight(String texto, int longitud) {
        if (texto == null)
            texto = "N/A";
        return String.format("%-" + longitud + "s", texto);
    }
}
