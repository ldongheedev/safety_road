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

    @PostMapping("/sync/police")
    public ResponseEntity<Map<String, Integer>> syncPolice() {
        int count = openDataService.syncPolice();
        return ResponseEntity.ok(Map.of("POLICE", count));
    }

    @PostMapping("/sync/illumination-light")
    public ResponseEntity<Map<String, Integer>> syncIlluminationLights() {
        int count = openDataService.syncIlluminationLights();
        return ResponseEntity.ok(Map.of("ILLUMINATION_LIGHT", count));
    }

    @PostMapping("/sync/seongnam-cctv")
    public ResponseEntity<Map<String, Integer>> syncSeongnamCctv() {
        int count = openDataService.syncSeongnamCctv();
        return ResponseEntity.ok(Map.of("SEONGNAM_CCTV", count));
    }
}
