package pt.notub.transaction.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.ticket.entity.TituloTransporte;
import pt.notub.payment.entity.Transacao;
import pt.notub.user.entity.Utilizador;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TituloTransporteRepository tituloTransporteRepository;
    private final UtilizadorRepository utilizadorRepository;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            TituloTransporteRepository tituloTransporteRepository,
                            UtilizadorRepository utilizadorRepository) {
        this.transacaoRepository = transacaoRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    public List<Transacao> getAllTransacoes() {
        return transacaoRepository.findAll();
    }

    public Optional<Transacao> getTransacaoById(Long id) {
        return transacaoRepository.findById(id);
    }

    public List<Transacao> getTransacoesByTitulo(Long tituloId) {
        return transacaoRepository.findByTituloId(tituloId);
    }

    public List<Transacao> getTransacoesByEstado(EstadoPagamento estado) {
        return transacaoRepository.findByEstadoPagamento(estado);
    }

    public List<Transacao> getTransacoesByUtilizador(Long utilizadorId) {
        return transacaoRepository.findByUtilizadorId(utilizadorId);
    }

    public Transacao createTransacao(Long tituloId, Long utilizadorId, String referenciaExterna) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        Transacao transacao = new Transacao();
        transacao.setTitulo(titulo);
        transacao.setUtilizador(utilizador);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setReferenciaExterna(referenciaExterna);

        return transacaoRepository.save(transacao);
    }

    public Transacao updateEstado(Long id, EstadoPagamento estado) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada"));
        transacao.setEstadoPagamento(estado);
        return transacaoRepository.save(transacao);
    }
}
