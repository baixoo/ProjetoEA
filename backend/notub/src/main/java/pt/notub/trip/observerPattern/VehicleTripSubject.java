package pt.notub.trip.observerPattern;

public interface VehicleTripSubject {
    // Método que vai atualizar a localização do veículo em tempo real
    void updateLocation(Long viagemId, Long novaParagemId);

    // Método para finalizar a viagem e notifivar os observadores
    void finishTrip(Long viagemId);

    // Métodos para gerir as subscrições dos observadores
    void addSubscription(Long viagemVeiculoId, Long userId);
    void removeSubscription(Long viagemVeiculoId, Long userId);
}
