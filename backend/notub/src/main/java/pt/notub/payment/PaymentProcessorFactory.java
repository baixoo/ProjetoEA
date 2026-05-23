package pt.notub.payment;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProcessorFactory {

    private final Map<String, PaymentProcessor> processors;

    public PaymentProcessorFactory(List<PaymentProcessor> processorList) {
        this.processors = new HashMap<>();
        for (PaymentProcessor processor : processorList) {
            processors.put(processor.getProviderName(), processor);
        }
    }

    public PaymentProcessor getProcessor(String provider) {
        PaymentProcessor processor = processors.get(provider.toLowerCase());
        if (processor == null) {
            throw new RuntimeException("Processador de pagamento nao encontrado: " + provider);
        }
        return processor;
    }

    public PaymentProcessor getDefault() {
        return getProcessor("stripe");
    }
}
