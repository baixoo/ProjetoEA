package pt.notub.payment;

public record PaymentStatus(
    String providerTransactionId,
    PaymentProviderStatus status,
    String rawStatus
) {}
