package pt.notub.email.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
    public void consumirEvento(Map<String, Object> evento) {
        String tipoEvento = (String) evento.get("tipoEvento");

        @SuppressWarnings("unchecked")
        Map<String, Object> dados = (Map<String, Object>) evento.get("dados");

        logger.info("Evento recebido: {}", tipoEvento);

        ComandoEvento comando = registoComandos.obterComando(tipoEvento);
        if (comando == null) {
            logger.warn("Sem comando registado para o tipo: {}", tipoEvento);
            return;
        }

        try {
            comando.executar(dados);
        } catch (Exception e) {
            logger.error("Erro ao executar comando para evento {}: {}", tipoEvento, e.getMessage(), e);
        }
    }
}
