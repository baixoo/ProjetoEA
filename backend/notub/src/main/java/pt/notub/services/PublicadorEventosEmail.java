package pt.notub.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.notub.config.RabbitMQConfig;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PublicadorEventosEmail {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeNome;
    private final String routingKeyUtilizadorCriado;
    private final String routingKeyRecuperacaoPassword;

    public PublicadorEventosEmail(RabbitTemplate rabbitTemplate,
                                  @Value("${notub.rabbitmq.exchange}") String exchangeNome,
                                  @Value("${notub.rabbitmq.routing-key.utilizador-criado}") String routingKeyUtilizadorCriado,
                                  @Value("${notub.rabbitmq.routing-key.recuperacao-password}") String routingKeyRecuperacaoPassword) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeNome = exchangeNome;
        this.routingKeyUtilizadorCriado = routingKeyUtilizadorCriado;
        this.routingKeyRecuperacaoPassword = routingKeyRecuperacaoPassword;
    }

    public void publicarUtilizadorCriado(Long utilizadorId, String email, String primeiroNome, String ultimoNome) {
        Map<String, Object> evento = Map.of(
            "tipoEvento", "UTILIZADOR_CRIADO",
            "carimboTemporal", LocalDateTime.now().toString(),
            "dados", Map.of(
                "utilizadorId", utilizadorId,
                "email", email,
                "primeiroNome", primeiroNome,
                "ultimoNome", ultimoNome != null ? ultimoNome : ""
            )
        );
        rabbitTemplate.convertAndSend(exchangeNome, routingKeyUtilizadorCriado, evento);
    }

    public void publicarRecuperacaoPassword(Long utilizadorId, String email, String primeiroNome, String token, String urlRecuperacao) {
        Map<String, Object> evento = Map.of(
            "tipoEvento", "RECUPERACAO_PASSWORD_PEDIDA",
            "carimboTemporal", LocalDateTime.now().toString(),
            "dados", Map.of(
                "utilizadorId", utilizadorId,
                "email", email,
                "primeiroNome", primeiroNome != null ? primeiroNome : "",
                "tokenRecuperacao", token,
                "urlRecuperacao", urlRecuperacao
            )
        );
        rabbitTemplate.convertAndSend(exchangeNome, routingKeyRecuperacaoPassword, evento);
    }
}
