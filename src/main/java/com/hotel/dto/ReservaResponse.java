package com.hotel.dto;

public class ReservaResponse {
    private String mensaje;
    private long noches;
    private String precioTotal; // Como String formateado igual que el original

    public ReservaResponse(String mensaje, long noches, String precioTotal) {
        this.mensaje = mensaje;
        this.noches = noches;
        this.precioTotal = precioTotal;
    }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public long getNoches() { return noches; }
    public void setNoches(long noches) { this.noches = noches; }
    public String getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(String precioTotal) { this.precioTotal = precioTotal; }
}
