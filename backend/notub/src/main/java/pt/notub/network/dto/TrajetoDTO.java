package pt.notub.network.dto;

import pt.notub.network.entity.Direcao;

import java.util.List;

public class TrajetoDTO {

    private Long id;
    private Direcao direcao;
    private LinhaSummaryDTO linha;
    private List<PontoPassagemDTO> pontosDePassagem;

    public TrajetoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Direcao getDirecao() { return direcao; }
    public void setDirecao(Direcao direcao) { this.direcao = direcao; }
    public LinhaSummaryDTO getLinha() { return linha; }
    public void setLinha(LinhaSummaryDTO linha) { this.linha = linha; }
    public List<PontoPassagemDTO> getPontosDePassagem() { return pontosDePassagem; }
    public void setPontosDePassagem(List<PontoPassagemDTO> pontosDePassagem) { this.pontosDePassagem = pontosDePassagem; }
}
