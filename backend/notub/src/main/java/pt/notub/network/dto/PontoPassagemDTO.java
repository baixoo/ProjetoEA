package pt.notub.network.dto;

public class PontoPassagemDTO {

    private Long id;
    private Long trajetoId;
    private int ordem;
    private ParagemDTO paragem;

    public PontoPassagemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTrajetoId() { return trajetoId; }
    public void setTrajetoId(Long trajetoId) { this.trajetoId = trajetoId; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public ParagemDTO getParagem() { return paragem; }
    public void setParagem(ParagemDTO paragem) { this.paragem = paragem; }
}
