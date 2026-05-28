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
import pt.notub.repositories.TarifaRepository;
import pt.notub.repositories.TransacaoRepository;
import pt.notub.repositories.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoService.class);
    private static final long CHECKOUT_TIMEOUT_MINUTES = 5;

    private final TransacaoRepository transacaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final TarifaRepository tarifaRepository;
    private final PaymentProcessorFactory processorFactory;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL}")
    private String frontendUrl;

    public PagamentoService(TransacaoRepository transacaoRepository,
                            UtilizadorRepository utilizadorRepository,
                            TarifaRepository tarifaRepository,
                            PaymentProcessorFactory processorFactory,
                            ApplicationEventPublisher eventPublisher) {
        this.transacaoRepository = transacaoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.tarifaRepository = tarifaRepository;
        this.processorFactory = processorFactory;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CheckoutResponse iniciarCheckout(String email, CheckoutRequest request) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));

        TipoProduto tipoProduto = TipoProduto.valueOf(request.getTipoProduto());

        var existingOpt = transacaoRepository.findActiveByUser(
                utilizador.getId(), EstadoPagamento.EM_CURSO);
        if (existingOpt.isPresent()) {
            Transacao existing = existingOpt.get();
            boolean withinTimeout = existing.getDataHora()
                    .plusMinutes(CHECKOUT_TIMEOUT_MINUTES)
                    .isAfter(LocalDateTime.now());

            if (withinTimeout) {
                PaymentProcessor processor = processorFactory.getDefault();
                PaymentStatus status = processor.checkStatus(existing.getStripeSessionId());

                if (status.status() == PaymentProviderStatus.SUCCESS) {
                    confirmarPagamento(existing.getId());
                    return new CheckoutResponse(existing.getId(), existing.getToken(), null, "CONCLUIDO");
                }
                if (status.status() == PaymentProviderStatus.EXPIRED
                        || status.status() == PaymentProviderStatus.DECLINED) {
                    existing.setEstadoPagamento(EstadoPagamento.CANCELADO);
                    transacaoRepository.save(existing);
                } else {
                    String url = processor.getSessionUrl(existing.getStripeSessionId());
                    if (url != null) {
                        return new CheckoutResponse(existing.getId(), existing.getToken(), url, "EM_CURSO");
                    }
                    existing.setEstadoPagamento(EstadoPagamento.CANCELADO);
                    transacaoRepository.save(existing);
                }
            } else {
                existing.setEstadoPagamento(EstadoPagamento.CANCELADO);
                transacaoRepository.save(existing);
            }
        }

        int nrZonas = (int) request.getZonaIds().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1L);

        TipoUtilizador tipoUtilizador = utilizador.getTipoUtilizador();
        if (tipoUtilizador == null) {
            tipoUtilizador = TipoUtilizador.ADULTO;
        }

        float unitPrice;
        if (tipoProduto == TipoProduto.BILHETE) {
            unitPrice = tarifaRepository
                    .findByCriteria(tipoUtilizador, null, nrZonas)
                    .orElseThrow(() -> new RuntimeException("Tarifa nao encontrada para bilhete"))
                    .getValor();
        } else {
            ModalidadePasse modalidade = ModalidadePasse.valueOf(request.getModalidade());
            unitPrice = tarifaRepository
                    .findByCriteria(tipoUtilizador, modalidade, nrZonas)
                    .orElseThrow(() -> new RuntimeException("Tarifa nao encontrada para passe"))
                    .getValor();
        }

        double totalValor;
        if (tipoProduto == TipoProduto.BILHETE) {
            int quantidade = request.getQuantidade() != null ? request.getQuantidade() : 1;
            totalValor = unitPrice * quantidade;
        } else {
            totalValor = unitPrice;
        }

        Transacao transacao = new Transacao();
        transacao.setUtilizador(utilizador);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setMetodoPagamento(MetodoPagamento.CARTAO);
        transacao.setTipoProduto(tipoProduto);
        transacao.setValor(totalValor);
        transacao.setToken(UUID.randomUUID().toString());
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
                totalValor, "eur", descricao, transacao.getId(),
                frontendUrl + "/tickets?stripe_success=true&t=" + transacao.getToken(),
                frontendUrl + "/tickets?stripe_cancel=true&t=" + transacao.getToken()
        );

        PaymentResult result = processor.initiatePayment(paymentRequest);

        transacao.setStripeSessionId(result.providerTransactionId());
        transacaoRepository.save(transacao);

        return new CheckoutResponse(transacao.getId(), transacao.getToken(), result.redirectUrl(), "EM_CURSO");
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

    public PagamentoStatusResponse verificarEstado(String token, Long userId) {
        Transacao transacao = transacaoRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Transacao nao encontrada"));

        if (!transacao.getUtilizador().getId().equals(userId)) {
            throw new RuntimeException("Acesso nao autorizado");
        }

        PagamentoStatusResponse response = new PagamentoStatusResponse();
        response.setTransacaoId(transacao.getId());
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
                confirmarPagamento(transacao.getId());
                response.setEstado(EstadoPagamento.CONCLUIDO.name());
                response.setTituloCriado(true);
            } else if (status.status() == PaymentProviderStatus.EXPIRED || status.status() == PaymentProviderStatus.DECLINED) {
                rejeitarPagamento(transacao.getId());
                response.setEstado(EstadoPagamento.REJEITADO.name());
            }
        }

        return response;
    }
}
