package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.entity.Paragem;

import java.util.List;

@Service
public class WalkingTransferPolicy {

    private static final double WALKING_SPEED_KMH = 4.0;
    private static final double TRANSFER_WALK_LIMIT_KM = 0.5; // 500m
    private static final double DIRECT_WALK_LIMIT_KM = 0.8;   // 800m
    private static final double INTERCHANGE_THRESHOLD_KM = 0.005; // 5 meters

    public boolean isTransferWalkAllowed(double distanceKm) {
        return distanceKm <= TRANSFER_WALK_LIMIT_KM;
    }

    public boolean isDirectWalkAllowed(double distanceKm) {
        return distanceKm <= DIRECT_WALK_LIMIT_KM;
    }

    public boolean isDirectWalkWorthShowing(int directWalkMinutes, List<RotaDTO> busRoutes) {
        if (busRoutes.isEmpty()) {
            return true;
        }
        int bestBusMinutes = busRoutes.stream()
                .mapToInt(RotaDTO::getTotalMinutos)
                .min()
                .orElse(Integer.MAX_VALUE);
        return directWalkMinutes <= bestBusMinutes;
    }

    public boolean shouldShowWalkSegment(Paragem from, Paragem to) {
        if (from == null || to == null) return false;
        if (from.getId().equals(to.getId())) return false;
        double dist = haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude()
        );
        return dist > INTERCHANGE_THRESHOLD_KM;
    }

    public int estimateWalkMinutes(Paragem from, Paragem to) {
        if (from == null || to == null) return 0;
        if (from.getId().equals(to.getId())) return 0;
        double dist = haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude()
        );
        if (dist <= INTERCHANGE_THRESHOLD_KM) return 0;
        return Math.max(1, (int) Math.round(dist / WALKING_SPEED_KMH * 60));
    }

    public double getDistanceKm(Paragem from, Paragem to) {
        if (from == null || to == null) return Double.MAX_VALUE;
        if (from.getId().equals(to.getId())) return 0.0;
        return haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude()
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
