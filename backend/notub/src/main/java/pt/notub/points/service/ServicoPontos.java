package pt.notub.points.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.ConflitoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.points.dto.HistoricoPontosDTO;
import pt.notub.points.dto.PontosSaldoResponse;
import pt.notub.points.dto.UtilizarPontosResponse;
import pt.notub.points.entity.HistoricoPontos;
import pt.notub.points.mapper.HistoricoPontosMapper;
import pt.notub.points.repository.HistoricoPontosRepository;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServicoPontos {

    private final UtilizadorRepository utilizadorRepository;
    private final HistoricoPontosRepository historicoPontosRepository;

    private static final int PONTOS_POR_VIAGEM = 10;
    private static final int PONTOS_POR_COMPRA = 5;

    public ServicoPontos(UtilizadorRepository utilizadorRepository, HistoricoPontosRepository historicoPontosRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.historicoPontosRepository = historicoPontosRepository;
    }

    public void atribuirPontosViagem(Long utilizadorId) {
        Utilizador utilizador = findUtilizador(utilizadorId);
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_VIAGEM);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_VIAGEM, "VIAGEM", "Pontos ganhos por viagem concluida");
    }

    public void atribuirPontosCompra(Long utilizadorId) {
        Utilizador utilizador = findUtilizador(utilizadorId);
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_COMPRA);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_COMPRA, "COMPRA", "Pontos ganhos por compra");
    }

    public List<HistoricoPontosDTO> getHistorico(Long utilizadorId) {
        return HistoricoPontosMapper.toDTOList(historicoPontosRepository.findByUtilizadorIdOrderByDataHoraDesc(utilizadorId));
    }

    public PontosSaldoResponse getSaldo(Long utilizadorId) {
        return new PontosSaldoResponse(findUtilizador(utilizadorId).getNrPontos());
    }

    public UtilizarPontosResponse utilizarPontos(Long utilizadorId, int pontos, String descricao) {
        if (pontos <= 0) {
            throw new PedidoInvalidoException("Pontos deve ser maior que zero");
        }
        Utilizador utilizador = findUtilizador(utilizadorId);
        if (utilizador.getNrPontos() < pontos) {
            throw new ConflitoException("Pontos insuficientes");
        }
        utilizador.setNrPontos(utilizador.getNrPontos() - pontos);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, -pontos, "RESGATE", descricao);
        return new UtilizarPontosResponse(
                utilizador.getNrPontos(),
                pontos + " pontos utilizados com sucesso"
        );
    }

    private Utilizador findUtilizador(Long utilizadorId) {
        return utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
    }

    private void registarHistorico(Utilizador utilizador, int pontos, String tipo, String descricao) {
        HistoricoPontos hp = new HistoricoPontos();
        hp.setUtilizador(utilizador);
        hp.setPontos(pontos);
        hp.setTipo(tipo);
        hp.setDescricao(descricao);
        hp.setDataHora(LocalDateTime.now());
        historicoPontosRepository.save(hp);
    }
}
