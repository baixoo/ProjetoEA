package pt.notub.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.TipoProduto;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;
import pt.notub.ticket.TicketService;

import java.util.Arrays;
import java.util.List;

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
                    .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
            List<Long> zonaIds = Arrays.stream(event.zonaIds().split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();

            if (TipoProduto.BILHETE.name().equals(event.tipoProduto())) {
                ticketService.buyTickets(utilizador.getEmail(), event.quantidade(), zonaIds);
            } else {
                ModalidadePasse modalidade = ModalidadePasse.valueOf(event.modalidade());
                ticketService.buyPasse(utilizador.getEmail(), modalidade, zonaIds);
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
