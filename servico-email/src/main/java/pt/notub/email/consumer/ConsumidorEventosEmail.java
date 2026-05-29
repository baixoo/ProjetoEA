package pt.notub.email.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Header;
import pt.notub.email.comando.ComandoEvento;
import pt.notub.email.comando.RegistoComandos;


import java.util.Map;

@Component
public class ConsumidorEventosEmail {

    private static final Logger logger = LoggerFactory.getLogger(ConsumidorEventosEmail.class);

    private final RegistoComandos registoComandos;

    public ConsumidorEventosEmail(RegistoComandos registoComandos) {
        this.registoComandos = registoComandos;
    }

    @RabbitListener(queues = "${notub.rabbitmq.fila}")
    public void consumirEvento(Map<String, Object> evento,
    @Header("amqp_receivedRoutingKey") String routingKey) {

        logger.info("Evento recebido com routing key: {}", routingKey);

        ComandoEvento comando = registoComandos.obterComando(routingKey);

        if (comando == null) {
            logger.warn("Sem comando registado para routing key: {}", routingKey);
            return;
        }

        try {
            comando.executar(evento);
        } catch (Exception e) {
            logger.error("Erro ao executar comando para routing key {}: {}", routingKey, e.getMessage(), e);
        }
    }
}
