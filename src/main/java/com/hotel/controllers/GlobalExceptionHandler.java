package com.hotel.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Controlador global de excepciones para asegurar la paridad de errores con el
 * proyecto original.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (message.startsWith("BAD_REQUEST:")) {
            status = HttpStatus.BAD_REQUEST;
            message = message.substring(13).trim();
        } else if (message.startsWith("UNAUTHORIZED:")) {
            status = HttpStatus.UNAUTHORIZED;
            message = message.substring(14).trim();
        } else if (message.startsWith("FORBIDDEN:")) {
            status = HttpStatus.FORBIDDEN;
            message = message.substring(11).trim();
        } else if (message.startsWith("NOT_FOUND:")) {
            status = HttpStatus.NOT_FOUND;
            message = message.substring(11).trim();
        } else if (message.startsWith("CONFLICT:")) {
            status = HttpStatus.CONFLICT;
            message = message.substring(10).trim();
        }

        return buildErrorResponse(status, message, request);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonSyntaxException(Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Error de formato en el JSON: Los tipos de datos enviados no son correctos o el JSON está mal formado.",
                request);
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            org.springframework.web.servlet.NoHandlerFoundException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "La ruta solicitada no existe o no soporta este método HTTP.",
                request);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio en la URL: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "El método HTTP " + ex.getMethod() + " no está soportado en esta ruta.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {
        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error inesperado en el servidor.", request);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> errorPayload = new java.util.LinkedHashMap<>();
        errorPayload.put("status", status.value());
        errorPayload.put("error", status.getReasonPhrase());
        errorPayload.put("mensaje", message);
        errorPayload.put("ruta", request.getRequestURI());

        return new ResponseEntity<>(errorPayload, status);
    }
}
