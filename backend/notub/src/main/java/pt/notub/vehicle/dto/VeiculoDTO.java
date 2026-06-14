package pt.notub.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import pt.notub.network.dto.PointDTO;
import pt.notub.vehicle.entity.TipoVeiculo;

public class VeiculoDTO {

    private Long id;
    private String matricula;
    private int nLugares;
    private int lotacaoAtual;
    private Integer tempoAtraso;
    private PointDTO localizacaoAtual;
    private TipoVeiculo tipo;
    private Long linhaId;
    private String linhaNome;

    public VeiculoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    @JsonProperty("nLugares")
    public int getnLugares() { return nLugares; }

    @JsonProperty("nLugares")
    public void setnLugares(int nLugares) { this.nLugares = nLugares; }

    public int getLotacaoAtual() { return lotacaoAtual; }
    public void setLotacaoAtual(int lotacaoAtual) { this.lotacaoAtual = lotacaoAtual; }
    public Integer getTempoAtraso() { return tempoAtraso; }
    public void setTempoAtraso(Integer tempoAtraso) { this.tempoAtraso = tempoAtraso; }
    public PointDTO getLocalizacaoAtual() { return localizacaoAtual; }
    public void setLocalizacaoAtual(PointDTO localizacaoAtual) { this.localizacaoAtual = localizacaoAtual; }
    public TipoVeiculo getTipo() { return tipo; }
    public void setTipo(TipoVeiculo tipo) { this.tipo = tipo; }
    public Long getLinhaId() { return linhaId; }
    public void setLinhaId(Long linhaId) { this.linhaId = linhaId; }
    public String getLinhaNome() { return linhaNome; }
    public void setLinhaNome(String linhaNome) { this.linhaNome = linhaNome; }
}
