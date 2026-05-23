package pt.notub.email.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${notub.rabbitmq.exchange}")
    private String exchangeNome;

    @Value("${notub.rabbitmq.fila}")
    private String filaNome;

    @Value("${notub.rabbitmq.routing-key.utilizador-criado}")
    private String routingKeyUtilizadorCriado;

    @Value("${notub.rabbitmq.routing-key.recuperacao-password}")
    private String routingKeyRecuperacaoPassword;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(exchangeNome, true, false);
    }

    @Bean
    public Queue emailFila() {
        return QueueBuilder.durable(filaNome).build();
    }

    @Bean
    public Binding bindingUtilizadorCriado(Queue emailFila, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailFila).to(emailExchange).with(routingKeyUtilizadorCriado);
    }

    @Bean
    public Binding bindingRecuperacaoPassword(Queue emailFila, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailFila).to(emailExchange).with(routingKeyRecuperacaoPassword);
    }
}
