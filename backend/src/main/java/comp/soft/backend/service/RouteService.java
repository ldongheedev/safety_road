package comp.soft.backend.service;

import comp.soft.backend.dto.RouteResponse;
import comp.soft.backend.dto.TmapRouteResult;
import comp.soft.backend.entity.DangerZone;
import comp.soft.backend.external.TmapClient;
import comp.soft.backend.repository.DangerZoneRepository;
import comp.soft.backend.repository.SafetyFacilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RouteService {

    private final TmapClient tmapClient;
    private final DangerZoneRepository dangerZoneRepository;
    private final SafetyFacilityRepository safetyFacilityRepository;

    public RouteService(TmapClient tmapClient,
                        DangerZoneRepository dangerZoneRepository,
                        SafetyFacilityRepository safetyFacilityRepository) {
        this.tmapClient = tmapClient;
        this.dangerZoneRepository = dangerZoneRepository;
        this.safetyFacilityRepository = safetyFacilityRepository;
    }

    public List<RouteResponse> getRoutes(double startLat, double startLng,
                                         double endLat, double endLng) {
        List<TmapRouteResult> tmapResults = tmapClient.getWalkingRoutes(startLat, startLng, endLat, endLng);

        List<RouteResponse> routes = tmapResults.stream()
                .map(result -> new RouteResponse(
                        UUID.randomUUID().toString(),
                        result.getCoordinates(),
                        result.getTotalDistance(),
                        result.getTotalTime(),
                        calculateSafetyScore(result.getCoordinates()),
                        false,
                        result.getSearchOption()
                ))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        routes.stream()
                .max(java.util.Comparator.comparingDouble(r -> r.getSafetyScore() != null ? r.getSafetyScore() : 0.0))
                .ifPresent(best -> {
                    int idx = routes.indexOf(best);
                    routes.set(idx, new RouteResponse(
                            best.getRouteId(),
                            best.getCoordinates(),
                            best.getTotalDistance(),
                            best.getTotalTime(),
                            best.getSafetyScore(),
                            true,
                            best.getSearchOption()
                    ));
                });

        return routes;
    }

    private double calculateSafetyScore(List<double[]> coordinates) {
        double dangerZoneScore = calcDangerZoneScore(coordinates);
        double facilityScore = calcFacilityDensityScore(coordinates);
        double combined = dangerZoneScore * 0.6 + facilityScore * 0.4;
        return Math.round(combined * 10.0) / 10.0;
    }

    private double calcDangerZoneScore(List<double[]> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) return 50.0;

        int sampleInterval = 10;
        double totalScore = 0.0;
        int sampleCount = 0;

        for (int i = 0; i < coordinates.size(); i += sampleInterval) {
            double lat = coordinates.get(i)[0];
            double lng = coordinates.get(i)[1];

            List<DangerZone> zones = dangerZoneRepository.findByPoint(lat, lng);
            if (zones.isEmpty()) {
                totalScore += 50.0;
            } else {
                double zoneScore = zones.stream()
                        .mapToDouble(z -> z.getSafetyScore().doubleValue())
                        .average()
                        .orElse(50.0);
                totalScore += zoneScore;
            }
            sampleCount++;
        }

        if (sampleCount == 0) return 50.0;
        return Math.round((totalScore / sampleCount) * 10.0) / 10.0;
    }

    private double calcFacilityDensityScore(List<double[]> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) return 50.0;

        final double RADIUS_DEGREES = 0.001;
        final double POLICE_RADIUS_DEGREES = 0.003;
        final int POLICE_WEIGHT = 5;
        int sampleInterval = 10;
        double totalScore = 0.0;
        int sampleCount = 0;

        for (int i = 0; i < coordinates.size(); i += sampleInterval) {
            double lat = coordinates.get(i)[0];
            double lng = coordinates.get(i)[1];

            int facilityCount = safetyFacilityRepository.countWithinRadius(lat, lng, RADIUS_DEGREES);
            int policeCount = safetyFacilityRepository.countPoliceWithinRadius(lat, lng, POLICE_RADIUS_DEGREES);
            int count = facilityCount + policeCount * POLICE_WEIGHT;
            totalScore += facilityCountToScore(count);
            sampleCount++;
        }

        if (sampleCount == 0) return 50.0;
        return Math.round((totalScore / sampleCount) * 10.0) / 10.0;
    }

    private double facilityCountToScore(int count) {
        if (count == 0) return 0.0;
        if (count <= 2) return 20.0 + count * 10.0;
        if (count <= 5) return 40.0 + (count - 2) * 7.0;
        if (count <= 10) return 61.0 + (count - 5) * 4.0;
        return Math.min(100.0, 81.0 + (count - 10) * 1.5);
    }
}
