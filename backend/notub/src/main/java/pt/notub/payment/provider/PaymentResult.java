package pt.notub.payment.provider;

public record PaymentResult(
    String providerTransactionId,
    String redirectUrl,
    PaymentProviderStatus status,
    String rawStatus
) {}
