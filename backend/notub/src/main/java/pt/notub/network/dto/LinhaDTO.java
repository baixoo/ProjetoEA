package pt.notub.network.dto;

import java.util.List;

public class LinhaDTO {

    private Long id;
    private String nome;
    private String identificadorServico;
    private List<TrajetoDTO> trajetos;

    public LinhaDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIdentificadorServico() { return identificadorServico; }
    public void setIdentificadorServico(String identificadorServico) { this.identificadorServico = identificadorServico; }
    public List<TrajetoDTO> getTrajetos() { return trajetos; }
    public void setTrajetos(List<TrajetoDTO> trajetos) { this.trajetos = trajetos; }
}
