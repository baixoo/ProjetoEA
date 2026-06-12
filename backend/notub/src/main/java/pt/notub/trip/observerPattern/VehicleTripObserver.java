package pt.notub.trip.observerPattern;

import pt.notub.trip.dto.ViagemVeiculoDTO;

public interface VehicleTripObserver {

    // O que faz quando a localização do veículo é atualizada
    void onLocationUpdate(Long viagemId, Long novaParagemId);

    // O que faz quando a viagem é finalizada
    void onTripFinished(Long viagemId);

    // === Adicionar métodos para gerir as subscrições dos observadores ===

    // Método para obter o ID do observador
    ViagemVeiculoDTO getViagemVeiculoById(Long id);
}
