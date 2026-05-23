package pt.notub.email.comando;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RegistoComandos {

    private final Map<String, ComandoEvento> registo;

    public RegistoComandos(List<ComandoEvento> comandos) {
        this.registo = comandos.stream()
                .collect(Collectors.toMap(ComandoEvento::routingKey, Function.identity()));
    }

    public ComandoEvento obterComando(String routingKey) {
        return registo.get(routingKey);
    }
}
