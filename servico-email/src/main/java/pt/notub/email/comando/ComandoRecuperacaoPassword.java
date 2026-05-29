package pt.notub.email.comando;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.notub.email.service.ServicoEnvioEmail;

import java.util.Map;

@Component
public class ComandoRecuperacaoPassword implements ComandoEvento {

    private final String routingKey;
    private final ServicoEnvioEmail servicoEnvioEmail;

    public ComandoRecuperacaoPassword(
        ServicoEnvioEmail servicoEnvioEmail,
        @Value("${notub.rabbitmq.routing-key.recuperacao-password}") String routingKey
    ) {
        this.servicoEnvioEmail = servicoEnvioEmail;
        this.routingKey = routingKey;
    }

    @Override
    public String routingKey() {
        return routingKey;
    }

    @Override
    public void executar(Map<String, Object> dados) {
        String email = (String) dados.get("email");
        String primeiroNome = (String) dados.get("primeiroNome");
        String urlRecuperacao = (String) dados.get("urlRecuperacao");

        servicoEnvioEmail.enviarRecuperacaoPassword(email, primeiroNome, urlRecuperacao);
    }
}
