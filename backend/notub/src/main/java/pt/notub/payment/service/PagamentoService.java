package pt.notub.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.notub.common.exception.AcessoNegadoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.payment.dto.CheckoutRequest;
import pt.notub.payment.dto.CheckoutResponse;
import pt.notub.payment.dto.PagamentoStatusResponse;
import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.payment.entity.MetodoPagamento;
import pt.notub.payment.entity.TipoProduto;
import pt.notub.payment.entity.Transacao;
import pt.notub.payment.event.PagamentoConfirmadoEvent;
import pt.notub.payment.event.PagamentoRejeitadoEvent;
import pt.notub.payment.provider.PaymentProcessor;
import pt.notub.payment.provider.PaymentProcessorFactory;
import pt.notub.payment.provider.PaymentProviderStatus;
import pt.notub.payment.provider.PaymentRequest;
import pt.notub.payment.provider.PaymentResult;
import pt.notub.payment.provider.PaymentStatus;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.tariff.repository.TarifaRepository;
import pt.notub.user.entity.TipoUtilizador;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoService.class);
    private static final long CHECKOUT_TIMEOUT_MINUTES = 5;
    private static final int MAX_ZONE_NUM = 3;

    private final TransacaoRepository transacaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final TarifaRepository tarifaRepository;
    private final PaymentProcessorFactory processorFactory;
    private final ApplicationEventPublisher eventPublisher;

    private final String frontendUrl;

    public PagamentoService(TransacaoRepository transacaoRepository,
                            UtilizadorRepository utilizadorRepository,
                            TarifaRepository tarifaRepository,
                            PaymentProcessorFactory processorFactory,
                            ApplicationEventPublisher eventPublisher,
                            @Value("${FRONTEND_URL}") String frontendUrl) {
        this.transacaoRepository = transacaoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.tarifaRepository = tarifaRepository;
        this.processorFactory = processorFactory;
        this.eventPublisher = eventPublisher;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public CheckoutResponse iniciarCheckout(String email, CheckoutRequest request) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        TipoProduto tipoProduto = parseTipoProduto(request.getTipoProduto());
        Long zonaId = request.getZonaId() != null ? request.getZonaId() : 1L;
        int nrZonas = requireNrZonas(zonaId);

        var existingOpt = transacaoRepository.findActiveByUser(
                utilizador.getId(), EstadoPagamento.EM_CURSO);
        if (existingOpt.isPresent()) {
            Transacao existing = existingOpt.get();
            boolean withinTimeout = existing.getDataHora()
                    .plusMinutes(CHECKOUT_TIMEOUT_MINUTES)
                    .isAfter(LocalDateTime.now());

            if (withinTimeout) {
                boolean mesmoProduto = existing.getTipoProduto() == tipoProduto;
                boolean mesmoValor = false;
                if (mesmoProduto) {
                    if (tipoProduto == TipoProduto.BILHETE) {
                        mesmoValor = existing.getZonaId().equals(zonaId);
                    } else {
                        mesmoValor = existing.getZonaId().equals(zonaId)
                                && existing.getModalidade() != null
                                && existing.getModalidade().equals(request.getModalidade());
                    }
                }

                if (!mesmoProduto || !mesmoValor) {
                    existing.setEstadoPagamento(EstadoPagamento.CANCELADO);
                    transacaoRepository.save(existing);
                } else {
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
                }
            } else {
                existing.setEstadoPagamento(EstadoPagamento.CANCELADO);
                transacaoRepository.save(existing);
            }
        }

        TipoUtilizador tipoUtilizador = utilizador.getTipoUtilizador();
        if (tipoUtilizador == null) {
            tipoUtilizador = TipoUtilizador.ADULTO;
        }

        float unitPrice;
        if (tipoProduto == TipoProduto.BILHETE) {
            unitPrice = tarifaRepository
                    .findByCriteria(tipoUtilizador, null, nrZonas)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada para bilhete"))
                    .getValor();
        } else {
            ModalidadePasse modalidade = parseModalidade(request.getModalidade());
            unitPrice = tarifaRepository
                    .findByCriteria(tipoUtilizador, modalidade, nrZonas)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada para passe"))
                    .getValor();
        }

        double totalValor;
        if (tipoProduto == TipoProduto.BILHETE) {
            int quantidade = request.getQuantidade() != null ? request.getQuantidade() : 1;
            if (quantidade <= 0) {
                throw new PedidoInvalidoException("Quantidade invalida");
            }
            totalValor = unitPrice * quantidade;
        } else {
            totalValor = unitPrice;
        }

        Transacao transacao = new Transacao();
        transacao.setUtilizador(utilizador);
        transacao.setDataHora(LocalDateTime.now());
        transacao.setEstadoPagamento(EstadoPagamento.EM_CURSO);
        transacao.setMetodoPagamento(MetodoPagamento.STRIPE);
        transacao.setTipoProduto(tipoProduto);
        transacao.setValor(totalValor);
        transacao.setToken(UUID.randomUUID().toString());
        transacao.setZonaId(zonaId);

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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada"));

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
                transacao.getZonaId()
        ));
    }

    @Transactional
    public void rejeitarPagamento(Long transacaoId) {
        Transacao transacao = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada"));

        transacao.setEstadoPagamento(EstadoPagamento.REJEITADO);
        transacaoRepository.save(transacao);

        eventPublisher.publishEvent(new PagamentoRejeitadoEvent(
                transacaoId, transacao.getUtilizador().getId()
        ));
    }

    public PagamentoStatusResponse verificarEstado(String token, Long userId) {
        Transacao transacao = transacaoRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transacao nao encontrada"));

        if (!transacao.getUtilizador().getId().equals(userId)) {
            throw new AcessoNegadoException();
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

    private int requireNrZonas(Long zonaId) {
        if (zonaId == null || zonaId < 1 || zonaId > MAX_ZONE_NUM) {
            throw new PedidoInvalidoException("Zona invalida");
        }
        return Math.toIntExact(zonaId);
    }

    private TipoProduto parseTipoProduto(String tipoProduto) {
        if (tipoProduto == null || tipoProduto.isBlank()) {
            throw new PedidoInvalidoException("Tipo de produto invalido");
        }
        try {
            return TipoProduto.valueOf(tipoProduto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Tipo de produto invalido");
        }
    }

    private ModalidadePasse parseModalidade(String modalidade) {
        if (modalidade == null || modalidade.isBlank()) {
            throw new PedidoInvalidoException("Modalidade invalida");
        }
        try {
            return ModalidadePasse.valueOf(modalidade.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Modalidade invalida");
        }
    }
}
