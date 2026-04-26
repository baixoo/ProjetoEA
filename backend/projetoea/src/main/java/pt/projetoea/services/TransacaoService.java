package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.EstadoPagamento;
import pt.projetoea.models.TituloTransporte;
import pt.projetoea.models.Transacao;
import pt.projetoea.repositories.TituloTransporteRepository;
import pt.projetoea.repositories.TransacaoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TituloTransporteRepository tituloTransporteRepository;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            TituloTransporteRepository tituloTransporteRepository) {
        this.transacaoRepository = transacaoRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
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

    public Transacao createTransacao(Long tituloId, String referenciaExterna) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RuntimeException("Titulo not found"));

        Transacao transacao = new Transacao();
        transacao.setTitulo(titulo);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setReferenciaExterna(referenciaExterna);

        return transacaoRepository.save(transacao);
    }

    public Transacao updateEstado(Long id, EstadoPagamento estado) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacao not found"));
        transacao.setEstadoPagamento(estado);
        return transacaoRepository.save(transacao);
    }
}
