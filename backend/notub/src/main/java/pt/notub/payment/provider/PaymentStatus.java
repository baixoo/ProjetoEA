package pt.notub.payment.provider;

public record PaymentStatus(
    String providerTransactionId,
    PaymentProviderStatus status,
    String rawStatus
) {}
