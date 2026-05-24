package comp.soft.backend.controller;

import comp.soft.backend.entity.DangerZone;
import comp.soft.backend.repository.DangerZoneRepository;
import comp.soft.backend.service.DangerZoneService;
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

    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Object>> recalculate() {
        int count = dangerZoneService.recalculate();
        return ResponseEntity.ok(Map.of(
                "message", "위험구역 재계산 완료",
                "totalZones", count
        ));
    }
}
