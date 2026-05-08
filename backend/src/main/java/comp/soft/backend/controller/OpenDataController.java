package comp.soft.backend.controller;

import comp.soft.backend.service.OpenDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/opendata")
public class OpenDataController {

    private final OpenDataService openDataService;

    public OpenDataController(OpenDataService openDataService) {
        this.openDataService = openDataService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> syncAll() {
        Map<String, Integer> result = openDataService.syncAll();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/cctv")
    public ResponseEntity<Map<String, Integer>> syncCctv() {
        int count = openDataService.syncCctv();
        return ResponseEntity.ok(Map.of("CCTV", count));
    }

    @PostMapping("/sync/security-light")
    public ResponseEntity<Map<String, Integer>> syncSecurityLights() {
        int count = openDataService.syncSecurityLights();
        return ResponseEntity.ok(Map.of("SECURITY_LIGHT", count));
    }

    @PostMapping("/sync/police-box")
    public ResponseEntity<Map<String, Integer>> syncPoliceBoxes() {
        int count = openDataService.syncPoliceBoxes();
        return ResponseEntity.ok(Map.of("POLICE_BOX", count));
    }

    @PostMapping("/sync/district-police")
    public ResponseEntity<Map<String, Integer>> syncDistrictPolice() {
        int count = openDataService.syncDistrictPolice();
        return ResponseEntity.ok(Map.of("DISTRICT_POLICE", count));
    }
}
