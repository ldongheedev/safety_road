package com.safetyroad.controller;

import com.safetyroad.service.OpenDataService;
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

    /**
     * 모든 공공데이터 동기화
     * POST /api/opendata/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> syncAll() {
        Map<String, Integer> result = openDataService.syncAll();
        return ResponseEntity.ok(result);
    }

    /**
     * CCTV 데이터만 동기화
     * POST /api/opendata/sync/cctv
     */
    @PostMapping("/sync/cctv")
    public ResponseEntity<Map<String, Integer>> syncCctv() {
        int count = openDataService.syncCctv();
        return ResponseEntity.ok(Map.of("CCTV", count));
    }

    /**
     * 보안등 데이터만 동기화
     * POST /api/opendata/sync/security-light
     */
    @PostMapping("/sync/security-light")
    public ResponseEntity<Map<String, Integer>> syncSecurityLights() {
        int count = openDataService.syncSecurityLights();
        return ResponseEntity.ok(Map.of("SECURITY_LIGHT", count));
    }
}
