package comp.soft.backend.controller;

import comp.soft.backend.entity.SafetyFacility;
import comp.soft.backend.repository.SafetyFacilityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/facilities")
public class SafetyFacilityController {

    private final SafetyFacilityRepository facilityRepository;

    public SafetyFacilityController(SafetyFacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getFacilitiesInBounds(
            @RequestParam double lat1,
            @RequestParam double lng1,
            @RequestParam double lat2,
            @RequestParam double lng2,
            @RequestParam(defaultValue = "200") int limit) {

        int safeLimit = Math.min(limit, 200);
        List<SafetyFacility> facilities = facilityRepository.findWithinBounds(lat1, lng1, lat2, lng2, safeLimit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SafetyFacility f : facilities) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("facilityType", f.getFacilityType());
            map.put("lat", f.getLocation().getY());
            map.put("lng", f.getLocation().getX());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}
