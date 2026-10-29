package com.hotel.dto;

public class HabitacionResponseDTO {
    private Integer id;
    private String numero;
    private String tipo;
    private Integer capacidad;
    private Boolean disponible;
    private String precio_base; // Formateado con moneda

    public HabitacionResponseDTO(Integer id, String numero, String tipo, Integer capacidad, Boolean disponible, String precio_base) {
        this.id = id;
        this.numero = numero;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.disponible = disponible;
        this.precio_base = precio_base;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
    public String getPrecio_base() { return precio_base; }
    public void setPrecio_base(String precio_base) { this.precio_base = precio_base; }
}
