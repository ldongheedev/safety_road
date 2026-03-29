package com.saferoute.controller;

import com.saferoute.dto.RouteResponse;
import com.saferoute.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/safe")
    public ResponseEntity<List<RouteResponse>> getSafeRoutes(
            @RequestParam double startLat,
            @RequestParam double startLng,
            @RequestParam double endLat,
            @RequestParam double endLng) {

        if (!isValidKoreaLat(startLat) || !isValidKoreaLat(endLat)
                || !isValidKoreaLng(startLng) || !isValidKoreaLng(endLng)) {
            return ResponseEntity.badRequest().build();
        }

        List<RouteResponse> routes = routeService.getRoutes(startLat, startLng, endLat, endLng);
        return ResponseEntity.ok(routes);
    }

    private boolean isValidKoreaLat(double lat) {
        return lat >= 33 && lat <= 43;
    }

    private boolean isValidKoreaLng(double lng) {
        return lng >= 124 && lng <= 132;
    }
}
