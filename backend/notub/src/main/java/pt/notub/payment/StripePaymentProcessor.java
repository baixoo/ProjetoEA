package pt.notub.payment;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentProcessor implements PaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentProcessor.class);

    @Value("${notub.stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentResult initiatePayment(PaymentRequest request) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(request.successUrl())
                    .setCancelUrl(request.cancelUrl())
                    .putMetadata("transacaoId", String.valueOf(request.internalTransactionId()))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.currency())
                                                    .setUnitAmount((long) Math.round(request.amount() * 100))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(request.description())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            logger.info("Stripe session criada: {} para transacao {}", session.getId(), request.internalTransactionId());
            return new PaymentResult(session.getId(), session.getUrl(), PaymentProviderStatus.PENDING, session.getPaymentStatus());
        } catch (Exception e) {
            logger.error("Erro ao criar sessao Stripe: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar sessao Stripe: " + e.getMessage());
        }
    }

    @Override
    public PaymentStatus checkStatus(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            String paymentStatus = session.getPaymentStatus();
            PaymentProviderStatus status = mapStatus(paymentStatus);
            logger.info("Stripe session {} payment_status: {}", sessionId, paymentStatus);
            return new PaymentStatus(sessionId, status, paymentStatus);
        } catch (Exception e) {
            logger.error("Erro ao verificar sessao Stripe {}: {}", sessionId, e.getMessage());
            return new PaymentStatus(sessionId, PaymentProviderStatus.UNKNOWN, "error");
        }
    }

    @Override
    public String getProviderName() {
        return "stripe";
    }

    private PaymentProviderStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) return PaymentProviderStatus.UNKNOWN;
        return switch (stripeStatus.toLowerCase()) {
            case "paid" -> PaymentProviderStatus.SUCCESS;
            case "unpaid" -> PaymentProviderStatus.PENDING;
            case "expired" -> PaymentProviderStatus.EXPIRED;
            case "canceled" -> PaymentProviderStatus.DECLINED;
            default -> PaymentProviderStatus.UNKNOWN;
        };
    }
}
