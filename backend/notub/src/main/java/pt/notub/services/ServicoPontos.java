package pt.notub.services;

import org.springframework.stereotype.Service;
import pt.notub.models.*;
import pt.notub.repositories.*;
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
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_VIAGEM);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_VIAGEM, "VIAGEM", "Pontos ganhos por viagem concluida");
    }

    public void atribuirPontosCompra(Long utilizadorId) {
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_COMPRA);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_COMPRA, "COMPRA", "Pontos ganhos por compra");
    }

    public void utilizarPontos(Long utilizadorId, int pontos, String descricao) {
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        if (utilizador.getNrPontos() < pontos) {
            throw new RuntimeException("Pontos insuficientes");
        }
        utilizador.setNrPontos(utilizador.getNrPontos() - pontos);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, -pontos, "RESGATE", descricao);
    }

    public List<HistoricoPontos> getHistorico(Long utilizadorId) {
        return historicoPontosRepository.findByUtilizadorIdOrderByDataHoraDesc(utilizadorId);
    }

    public Utilizador getUtilizadorAtualizado(Long utilizadorId) {
        return utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
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
