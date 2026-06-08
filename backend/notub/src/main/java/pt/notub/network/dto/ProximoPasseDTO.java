package pt.notub.network.dto;

public class ProximoPasseDTO {
    private String hora;
    private int esperaMinutos;

    public ProximoPasseDTO() {}
    public ProximoPasseDTO(String hora, int esperaMinutos) {
        this.hora = hora;
        this.esperaMinutos = esperaMinutos;
    }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public int getEsperaMinutos() { return esperaMinutos; }
    public void setEsperaMinutos(int esperaMinutos) { this.esperaMinutos = esperaMinutos; }
}
