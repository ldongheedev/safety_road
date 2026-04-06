package com.saferoute.service;

import com.saferoute.dto.RouteResponse;
import com.saferoute.dto.TmapRouteResult;
import com.saferoute.external.TmapClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RouteService {

    private final TmapClient tmapClient;

    public RouteService(TmapClient tmapClient) {
        this.tmapClient = tmapClient;
    }

    public List<RouteResponse> getRoutes(double startLat, double startLng,
                                         double endLat, double endLng) {
        List<TmapRouteResult> tmapResults = tmapClient.getWalkingRoutes(startLat, startLng, endLat, endLng);

        return tmapResults.stream()
                .map(result -> new RouteResponse(
                        UUID.randomUUID().toString(),
                        result.getCoordinates(),
                        result.getTotalDistance(),
                        result.getTotalTime(),
                        null,
                        false,
                        result.getSearchOption()
                ))
                .toList();
    }
}
