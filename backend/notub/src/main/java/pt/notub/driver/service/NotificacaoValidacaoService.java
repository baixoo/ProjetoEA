package pt.notub.driver.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pt.notub.driver.dto.NotificacaoValidacaoDTO;

@Service
public class NotificacaoValidacaoService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificacaoValidacaoService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notificarMotorista(NotificacaoValidacaoDTO notificacao) {
        String destination = "/topic/bus." + notificacao.getVeiculoId();
        messagingTemplate.convertAndSend(destination, notificacao);
    }
}
