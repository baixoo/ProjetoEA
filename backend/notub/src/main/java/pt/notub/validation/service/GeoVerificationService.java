package pt.notub.validation.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Paragem;
import pt.notub.vehicle.entity.Point;
import pt.notub.network.repository.ParagemRepository;

@Service
public class GeoVerificationService {

    private static final double MAX_DISTANCE_METERS = 500.0;

    private final ParagemRepository paragemRepository;

    public GeoVerificationService(ParagemRepository paragemRepository) {
        this.paragemRepository = paragemRepository;
    }

    public boolean isProximo(Long paragemId, double userLat, double userLng) {
        Paragem paragem = paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));

        Point loc = paragem.getLocalizacao();
        if (loc == null) return true;

        double distance = haversine(userLat, userLng, loc.getLatitude(), loc.getLongitude());
        return distance <= MAX_DISTANCE_METERS;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
