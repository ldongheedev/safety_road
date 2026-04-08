package com.safetyroad.service;

import com.safetyroad.entity.DangerZone;
import com.safetyroad.repository.DangerZoneRepository;
import com.safetyroad.repository.SafetyFacilityRepository;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DangerZoneService {

    private static final Logger log = LoggerFactory.getLogger(DangerZoneService.class);
    private static final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    // 분당구 영역 (대략적 범위)
    private static final double MIN_LAT = 37.33;
    private static final double MAX_LAT = 37.42;
    private static final double MIN_LNG = 127.04;
    private static final double MAX_LNG = 127.16;

    // 격자 크기: 약 300m (위도 0.0027 ≈ 300m, 경도 0.0034 ≈ 300m)
    private static final double GRID_LAT = 0.0027;
    private static final double GRID_LNG = 0.0034;

    private final DangerZoneRepository dangerZoneRepository;
    private final SafetyFacilityRepository facilityRepository;

    public DangerZoneService(DangerZoneRepository dangerZoneRepository,
                             SafetyFacilityRepository facilityRepository) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.facilityRepository = facilityRepository;
    }

    /**
     * 안전 시설 밀도 기반으로 위험구역을 재계산
     * - 격자별 CCTV + 보안등 + 가로등 개수를 세고
     * - 시설이 적은 구역일수록 위험도가 높음
     */
    @Transactional
    public int recalculate() {
        log.info("위험구역 재계산 시작");

        // 기존 위험구역 삭제
        dangerZoneRepository.deleteAll();

        List<DangerZone> zones = new ArrayList<>();

        for (double lat = MIN_LAT; lat < MAX_LAT; lat += GRID_LAT) {
            for (double lng = MIN_LNG; lng < MAX_LNG; lng += GRID_LNG) {
                double lat2 = lat + GRID_LAT;
                double lng2 = lng + GRID_LNG;

                // 격자 내 시설 개수 조회
                int count = facilityRepository.countWithinBounds(lat, lng, lat2, lng2);

                // 안전 점수 계산: 시설이 많을수록 점수가 높음
                // 0개=0점, 1~2개=20~40점, 3~5개=40~60점, 6~10개=60~80점, 11개+=80~100점
                double safetyScore = calculateSafetyScore(count);
                String riskLevel = getRiskLevel(safetyScore);

                // 시설이 0개인 외곽 빈 구역은 제외 (데이터 없는 영역)
                // 최소 주변에 시설이 있는 영역만 포함
                if (count == 0 && !hasNearbyFacilities(lat, lng, lat2, lng2)) {
                    continue;
                }

                Polygon polygon = createGridPolygon(lat, lng, lat2, lng2);

                DangerZone zone = new DangerZone();
                zone.setGridPolygon(polygon);
                zone.setSafetyScore(BigDecimal.valueOf(safetyScore).setScale(2, RoundingMode.HALF_UP));
                zone.setFacilityCount(count);
                zone.setRiskLevel(riskLevel);
                zones.add(zone);
            }
        }

        dangerZoneRepository.saveAll(zones);
        log.info("위험구역 재계산 완료: {}개 격자", zones.size());
        return zones.size();
    }

    private double calculateSafetyScore(int facilityCount) {
        if (facilityCount == 0) return 10.0;
        if (facilityCount <= 2) return 20.0 + facilityCount * 10.0;
        if (facilityCount <= 5) return 40.0 + (facilityCount - 2) * 7.0;
        if (facilityCount <= 10) return 61.0 + (facilityCount - 5) * 4.0;
        return Math.min(100.0, 81.0 + (facilityCount - 10) * 1.5);
    }

    private String getRiskLevel(double safetyScore) {
        if (safetyScore <= 30) return "HIGH";
        if (safetyScore <= 60) return "MEDIUM";
        return "LOW";
    }

    /**
     * 주변 1칸 확장 범위에 시설이 있는지 확인 (완전히 빈 외곽 제외용)
     */
    private boolean hasNearbyFacilities(double lat1, double lng1, double lat2, double lng2) {
        return facilityRepository.countWithinBounds(
                lat1 - GRID_LAT, lng1 - GRID_LNG,
                lat2 + GRID_LAT, lng2 + GRID_LNG
        ) > 0;
    }

    private Polygon createGridPolygon(double lat1, double lng1, double lat2, double lng2) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(lng1, lat1),
                new Coordinate(lng2, lat1),
                new Coordinate(lng2, lat2),
                new Coordinate(lng1, lat2),
                new Coordinate(lng1, lat1)  // 닫기
        };
        return gf.createPolygon(coords);
    }
}
