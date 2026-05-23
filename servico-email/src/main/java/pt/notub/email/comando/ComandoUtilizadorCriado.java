package pt.notub.email.comando;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.notub.email.service.ServicoEnvioEmail;

import java.util.Map;

@Component
public class ComandoUtilizadorCriado implements ComandoEvento {

    private final String routingKey;
    private final ServicoEnvioEmail servicoEnvioEmail;

    public ComandoUtilizadorCriado(
            ServicoEnvioEmail servicoEnvioEmail,
            @Value("${notub.rabbitmq.routing-key.utilizador-criado}") String routingKey
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
        String ultimoNome = (String) dados.get("ultimoNome");

        servicoEnvioEmail.enviarBoasVindas(email, primeiroNome, ultimoNome);
    }
}