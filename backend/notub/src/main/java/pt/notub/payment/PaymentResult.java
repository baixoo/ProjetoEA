package pt.notub.payment;

public record PaymentResult(
    String providerTransactionId,
    String redirectUrl,
    PaymentProviderStatus status,
    String rawStatus
) {}
