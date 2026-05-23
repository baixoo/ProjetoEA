package pt.notub.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.notub.dto.request.CheckoutRequest;
import pt.notub.dto.response.CheckoutResponse;
import pt.notub.dto.response.PagamentoStatusResponse;
import pt.notub.models.*;
import pt.notub.payment.*;
import pt.notub.repositories.TransacaoRepository;
import pt.notub.repositories.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoService.class);

    private final TransacaoRepository transacaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final PaymentProcessorFactory processorFactory;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:https://localhost}")
    private String frontendUrl;

    public PagamentoService(TransacaoRepository transacaoRepository,
                            UtilizadorRepository utilizadorRepository,
                            PaymentProcessorFactory processorFactory,
                            ApplicationEventPublisher eventPublisher) {
        this.transacaoRepository = transacaoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.processorFactory = processorFactory;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CheckoutResponse iniciarCheckout(String email, CheckoutRequest request) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));

        TipoProduto tipoProduto = TipoProduto.valueOf(request.getTipoProduto());

        Transacao transacao = new Transacao();
        transacao.setUtilizador(utilizador);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setMetodoPagamento(MetodoPagamento.CARTAO);
        transacao.setTipoProduto(tipoProduto);
        transacao.setValor(request.getValor());
        transacao.setZonaIds(request.getZonaIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));

        if (tipoProduto == TipoProduto.BILHETE) {
            transacao.setQuantidade(request.getQuantidade());
        } else {
            transacao.setModalidade(request.getModalidade());
        }

        String merchantTxId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        transacao.setReferenciaExterna(merchantTxId);
        transacao = transacaoRepository.save(transacao);

        PaymentProcessor processor = processorFactory.getDefault();
        String descricao = tipoProduto == TipoProduto.BILHETE
                ? "Bilhetes NoTUB x" + transacao.getQuantidade()
                : "Passe NoTUB " + transacao.getModalidade();

        PaymentRequest paymentRequest = new PaymentRequest(
                request.getValor(), "eur", descricao, transacao.getId(),
                frontendUrl + "/tickets?stripe_success=true&transacao_id=" + transacao.getId(),
                frontendUrl + "/tickets?stripe_cancel=true&transacao_id=" + transacao.getId()
        );

        PaymentResult result = processor.initiatePayment(paymentRequest);

        transacao.setStripeSessionId(result.providerTransactionId());
        transacaoRepository.save(transacao);

        return new CheckoutResponse(transacao.getId(), result.redirectUrl(), "EM_CURSO");
    }

    @Transactional
    public void confirmarPagamento(Long transacaoId) {
        Transacao transacao = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new RuntimeException("Transacao nao encontrada"));

        if (transacao.getEstadoPagamento() == EstadoPagamento.CONCLUIDO) {
            return;
        }

        transacao.setEstadoPagamento(EstadoPagamento.CONCLUIDO);
        transacaoRepository.save(transacao);

        eventPublisher.publishEvent(new PagamentoConfirmadoEvent(
                transacaoId,
                transacao.getUtilizador().getId(),
                transacao.getTipoProduto().name(),
                transacao.getQuantidade(),
                transacao.getModalidade(),
                transacao.getZonaIds()
        ));
    }

    @Transactional
    public void rejeitarPagamento(Long transacaoId) {
        Transacao transacao = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new RuntimeException("Transacao nao encontrada"));

        transacao.setEstadoPagamento(EstadoPagamento.REJEITADO);
        transacaoRepository.save(transacao);

        eventPublisher.publishEvent(new PagamentoRejeitadoEvent(
                transacaoId, transacao.getUtilizador().getId()
        ));
    }

    public PagamentoStatusResponse verificarEstado(Long transacaoId) {
        Transacao transacao = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new RuntimeException("Transacao nao encontrada"));

        PagamentoStatusResponse response = new PagamentoStatusResponse();
        response.setTransacaoId(transacaoId);
        response.setEstado(transacao.getEstadoPagamento().name());
        response.setTituloCriado(transacao.getTitulo() != null);

        if (transacao.getEstadoPagamento() == EstadoPagamento.CONCLUIDO) {
            response.setPagamentoExternoStatus("Success");
            return response;
        }

        if (transacao.getEstadoPagamento() == EstadoPagamento.REJEITADO) {
            response.setPagamentoExternoStatus("Declined");
            return response;
        }

        if (transacao.getEstadoPagamento() == EstadoPagamento.EM_CURSO && transacao.getStripeSessionId() != null) {
            PaymentProcessor processor = processorFactory.getDefault();
            PaymentStatus status = processor.checkStatus(transacao.getStripeSessionId());
            response.setPagamentoExternoStatus(status.rawStatus());

            if (status.status() == PaymentProviderStatus.SUCCESS) {
                confirmarPagamento(transacaoId);
                response.setEstado(EstadoPagamento.CONCLUIDO.name());
                response.setTituloCriado(true);
            } else if (status.status() == PaymentProviderStatus.EXPIRED || status.status() == PaymentProviderStatus.DECLINED) {
                rejeitarPagamento(transacaoId);
                response.setEstado(EstadoPagamento.REJEITADO.name());
            }
        }

        return response;
    }
}
