package com.safetyroad.service;

import com.safetyroad.dto.RouteResponse;
import com.safetyroad.dto.TmapRouteResult;
import com.safetyroad.entity.DangerZone;
import com.safetyroad.external.TmapClient;
import com.safetyroad.repository.DangerZoneRepository;
import com.safetyroad.repository.SafetyFacilityRepository;
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

        // 각 경로의 안전 점수 계산
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

        // 가장 안전한 경로에 isRecommended = true 설정
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

    /**
     * 경로 좌표 리스트를 받아 안전 점수(0~100)를 계산한다.
     * 2단계: 위험구역 기반 점수
     * 3단계: 안전시설 밀도 기반 점수
     * 4단계: 두 점수 조합
     */
    /**
     * 위험구역 점수(60%) + 시설밀도 점수(40%) 가중 평균
     */
    private double calculateSafetyScore(List<double[]> coordinates) {
        double dangerZoneScore = calcDangerZoneScore(coordinates);
        double facilityScore = calcFacilityDensityScore(coordinates);
        double combined = dangerZoneScore * 0.6 + facilityScore * 0.4;
        return Math.round(combined * 10.0) / 10.0;
    }

    /**
     * 경로 좌표를 10개마다 1개씩 샘플링해서
     * 각 좌표가 속한 위험구역의 safetyScore 평균을 반환한다.
     * 위험구역이 없는 좌표는 기본값 50점으로 처리한다.
     */
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

    /**
     * 경로 좌표를 10개마다 1개씩 샘플링해서
     * 각 좌표 반경 100m 내 안전시설 수의 평균을 점수(0~100)로 환산한다.
     * 반경 100m ≈ 위도/경도 0.001도
     */
    private double calcFacilityDensityScore(List<double[]> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) return 50.0;

        // 100m ≈ 0.001도 (위도 기준)
        final double RADIUS_DEGREES = 0.001;
        int sampleInterval = 10;
        double totalScore = 0.0;
        int sampleCount = 0;

        for (int i = 0; i < coordinates.size(); i += sampleInterval) {
            double lat = coordinates.get(i)[0];
            double lng = coordinates.get(i)[1];

            int count = safetyFacilityRepository.countWithinRadius(lat, lng, RADIUS_DEGREES);
            totalScore += facilityCountToScore(count);
            sampleCount++;
        }

        if (sampleCount == 0) return 50.0;
        return Math.round((totalScore / sampleCount) * 10.0) / 10.0;
    }

    /**
     * 반경 내 시설 수 → 0~100 점수 환산
     * 0개=0점, 1~2개=20~40점, 3~5개=40~60점, 6~10개=60~80점, 11개+=80~100점
     */
    private double facilityCountToScore(int count) {
        if (count == 0) return 0.0;
        if (count <= 2) return 20.0 + count * 10.0;
        if (count <= 5) return 40.0 + (count - 2) * 7.0;
        if (count <= 10) return 61.0 + (count - 5) * 4.0;
        return Math.min(100.0, 81.0 + (count - 10) * 1.5);
    }
}
