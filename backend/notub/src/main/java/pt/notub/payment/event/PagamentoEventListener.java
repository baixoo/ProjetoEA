package pt.notub.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.payment.entity.TipoProduto;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.ticket.service.TicketService;

@Component
public class PagamentoEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoEventListener.class);

    private final TicketService ticketService;
    private final UtilizadorRepository utilizadorRepository;

    public PagamentoEventListener(TicketService ticketService, UtilizadorRepository utilizadorRepository) {
        this.ticketService = ticketService;
        this.utilizadorRepository = utilizadorRepository;
    }

    @EventListener
    @Transactional
    public void onPagamentoConfirmado(PagamentoConfirmadoEvent event) {
        try {
            Utilizador utilizador = utilizadorRepository.findById(event.utilizadorId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

            if (TipoProduto.BILHETE.name().equals(event.tipoProduto())) {
                ticketService.buyTickets(utilizador.getEmail(), event.quantidade(), event.zonaId());
            } else {
                ModalidadePasse modalidade = ModalidadePasse.valueOf(event.modalidade());
                ticketService.buyPasse(utilizador.getEmail(), modalidade, event.zonaId());
            }
            logger.info("Titulo criado apos pagamento confirmado para transacao {}", event.transacaoId());
        } catch (Exception e) {
            logger.error("Erro ao criar titulo apos pagamento: {}", e.getMessage());
        }
    }

    @EventListener
    public void onPagamentoRejeitado(PagamentoRejeitadoEvent event) {
        logger.warn("Pagamento rejeitado para transacao {} do utilizador {}", event.transacaoId(), event.utilizadorId());
    }
}
