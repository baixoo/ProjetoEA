package pt.notub.transaction.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.payment.entity.Transacao;
import pt.notub.ticket.entity.TituloTransporte;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.transaction.dto.CreateTransacaoRequest;
import pt.notub.transaction.dto.TransacaoDTO;
import pt.notub.transaction.dto.UpdateEstadoPagamentoRequest;
import pt.notub.transaction.mapper.TransacaoMapper;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<TransacaoDTO> getAllTransacoes() {
        return TransacaoMapper.toDTOList(transacaoRepository.findAll());
    }

    public TransacaoDTO getTransacaoById(Long id) {
        return TransacaoMapper.toDTO(transacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada")));
    }

    public List<TransacaoDTO> getTransacoesByTitulo(Long tituloId) {
        return TransacaoMapper.toDTOList(transacaoRepository.findByTituloId(tituloId));
    }

    public List<TransacaoDTO> getTransacoesByEstado(String estado) {
        return TransacaoMapper.toDTOList(transacaoRepository.findByEstadoPagamento(parseEstado(estado)));
    }

    public List<TransacaoDTO> getTransacoesByUtilizador(Long utilizadorId) {
        return TransacaoMapper.toDTOList(transacaoRepository.findByUtilizadorId(utilizadorId));
    }

    public TransacaoDTO createTransacao(CreateTransacaoRequest request) {
        TituloTransporte titulo = tituloTransporteRepository.findById(request.tituloId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
        Utilizador utilizador = utilizadorRepository.findById(request.utilizadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        Transacao transacao = new Transacao();
        transacao.setTitulo(titulo);
        transacao.setUtilizador(utilizador);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setReferenciaExterna(request.referenciaExterna());

        return TransacaoMapper.toDTO(transacaoRepository.save(transacao));
    }

    public TransacaoDTO updateEstado(Long id, UpdateEstadoPagamentoRequest request) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada"));
        transacao.setEstadoPagamento(parseEstado(request.estado()));
        return TransacaoMapper.toDTO(transacaoRepository.save(transacao));
    }

    private EstadoPagamento parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new PedidoInvalidoException("Estado invalido");
        }
        try {
            return EstadoPagamento.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Estado invalido");
        }
    }
}
