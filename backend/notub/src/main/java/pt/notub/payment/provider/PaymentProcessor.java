package pt.notub.payment.provider;

public interface PaymentProcessor {
    PaymentResult initiatePayment(PaymentRequest request);
    PaymentStatus checkStatus(String transactionRef);
    String getSessionUrl(String sessionId);
    String getProviderName();
}
