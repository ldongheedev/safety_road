package com.saferoute.controller;

import com.saferoute.entity.DangerZone;
import com.saferoute.repository.DangerZoneRepository;
import com.saferoute.service.DangerZoneService;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/danger-zones")
public class DangerZoneController {

    private final DangerZoneRepository dangerZoneRepository;
    private final DangerZoneService dangerZoneService;

    public DangerZoneController(DangerZoneRepository dangerZoneRepository,
                                DangerZoneService dangerZoneService) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.dangerZoneService = dangerZoneService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDangerZones() {
        List<DangerZone> zones = dangerZoneRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (DangerZone zone : zones) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", zone.getId());
            map.put("safetyScore", zone.getSafetyScore());
            map.put("facilityCount", zone.getFacilityCount());
            map.put("riskLevel", zone.getRiskLevel());

            Coordinate[] coords = zone.getGridPolygon().getCoordinates();
            List<double[]> bounds = new ArrayList<>();
            for (Coordinate c : coords) {
                bounds.add(new double[]{c.y, c.x});
            }
            map.put("bounds", bounds);

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 시설 밀도 기반 위험구역 재계산
     * POST /api/danger-zones/recalculate
     */
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Object>> recalculate() {
        int count = dangerZoneService.recalculate();
        return ResponseEntity.ok(Map.of(
                "message", "위험구역 재계산 완료",
                "totalZones", count
        ));
    }
}
