package pt.notub.trip.observerPattern;

public interface VehicleTripSubject {
    // Método que vai atualizar a localização do veículo em tempo real
    void notifySubscribers(Long viagemId, Long novaParagemId);;
    void notifySubscribers(Long viagemId);

    // Métodos para gerir as subscrições dos observadores
    void addSubscription(Long viagemVeiculoId, Long userId);
    void removeSubscription(Long viagemVeiculoId, Long userId);
}
