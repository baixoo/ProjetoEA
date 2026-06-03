package pt.notub.payment;

public record PaymentRequest(
    double amount,
    String currency,
    String description,
    Long internalTransactionId,
    String successUrl,
    String cancelUrl
) {}
