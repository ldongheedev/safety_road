package comp.soft.backend.service;

import comp.soft.backend.entity.DangerZone;
import comp.soft.backend.repository.DangerZoneRepository;
import comp.soft.backend.repository.SafetyFacilityRepository;
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

    private static final double MIN_LAT = 37.33;
    private static final double MAX_LAT = 37.52;
    private static final double MIN_LNG = 127.04;
    private static final double MAX_LNG = 127.16;

    private static final double GRID_LAT = 0.0027;
    private static final double GRID_LNG = 0.0034;

    private final DangerZoneRepository dangerZoneRepository;
    private final SafetyFacilityRepository facilityRepository;

    public DangerZoneService(DangerZoneRepository dangerZoneRepository,
                             SafetyFacilityRepository facilityRepository) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.facilityRepository = facilityRepository;
    }

    @Transactional
    public int recalculate() {
        log.info("위험구역 재계산 시작");

        dangerZoneRepository.deleteAll();

        List<DangerZone> zones = new ArrayList<>();

        for (double lat = MIN_LAT; lat < MAX_LAT; lat += GRID_LAT) {
            for (double lng = MIN_LNG; lng < MAX_LNG; lng += GRID_LNG) {
                double lat2 = lat + GRID_LAT;
                double lng2 = lng + GRID_LNG;

                int facilityCount = facilityRepository.countWithinBounds(lat, lng, lat2, lng2);
                int policeCount = facilityRepository.countPoliceWithinBounds(lat, lng, lat2, lng2);
                int count = facilityCount + policeCount * 5;

                double safetyScore = calculateSafetyScore(count);
                String riskLevel = getRiskLevel(safetyScore);

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
                new Coordinate(lng1, lat1)
        };
        return gf.createPolygon(coords);
    }
}
