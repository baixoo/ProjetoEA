package pt.notub.zone.dto;

public class ZonaDTO {

    private Long id;
    private int num;
    private String nome;

    public ZonaDTO() {}

    public ZonaDTO(Long id, int num, String nome) {
        this.id = id;
        this.num = num;
        this.nome = nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
