package com.saferoute.external;

import com.saferoute.dto.TmapRouteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;

@Component
public class TmapClient {

    private static final Logger log = LoggerFactory.getLogger(TmapClient.class);

    private final WebClient webClient;
    private final String tmapAppKey;

    private static final Map<Integer, String> SEARCH_OPTIONS = Map.of(
            0, "추천",
            4, "대로우선",
            10, "최단"
    );

    public TmapClient(WebClient.Builder webClientBuilder,
                      @Value("${tmap.app-key}") String tmapAppKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://apis.openapi.sk.com")
                .build();
        this.tmapAppKey = tmapAppKey;
    }

    public List<TmapRouteResult> getWalkingRoutes(double startLat, double startLng,
                                                   double endLat, double endLng) {
        List<TmapRouteResult> results = new ArrayList<>();

        for (var entry : SEARCH_OPTIONS.entrySet()) {
            try {
                TmapRouteResult result = requestRoute(startLat, startLng, endLat, endLng,
                        entry.getKey(), entry.getValue());
                if (result != null) {
                    results.add(result);
                }
            } catch (WebClientResponseException e) {
                log.warn("TMap API 호출 실패 (option={}): {} {}", entry.getKey(), e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.warn("TMap API 오류 (option={}): {}", entry.getKey(), e.getMessage());
            }
        }

        return results;
    }

    private TmapRouteResult requestRoute(double startLat, double startLng,
                                          double endLat, double endLng,
                                          int searchOption, String optionName) {
        Map<String, Object> body = Map.of(
                "startX", String.valueOf(startLng),
                "startY", String.valueOf(startLat),
                "endX", String.valueOf(endLng),
                "endY", String.valueOf(endLat),
                "startName", "출발지",
                "endName", "도착지",
                "searchOption", String.valueOf(searchOption)
        );

        Map response = webClient.post()
                .uri("/tmap/routes/pedestrian?version=1")
                .header("appKey", tmapAppKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .block();

        if (response == null) return null;

        return parseResponse(response, optionName);
    }

    @SuppressWarnings("unchecked")
    private TmapRouteResult parseResponse(Map response, String optionName) {
        List<Map> features = (List<Map>) response.get("features");
        if (features == null || features.isEmpty()) return null;

        List<double[]> allCoordinates = new ArrayList<>();
        int totalDistance = 0;
        int totalTime = 0;

        for (Map feature : features) {
            Map geometry = (Map) feature.get("geometry");
            Map properties = (Map) feature.get("properties");

            // totalDistance, totalTime은 첫 번째 Point feature의 properties에 있음
            if (properties.containsKey("totalDistance") && totalDistance == 0) {
                totalDistance = toInt(properties.get("totalDistance"));
                totalTime = toInt(properties.get("totalTime"));
            }

            String type = (String) geometry.get("type");
            if ("LineString".equals(type)) {
                List<List<Number>> coords = (List<List<Number>>) geometry.get("coordinates");
                for (List<Number> coord : coords) {
                    double lng = coord.get(0).doubleValue();
                    double lat = coord.get(1).doubleValue();
                    allCoordinates.add(new double[]{lat, lng});
                }
            }
        }

        if (allCoordinates.isEmpty()) return null;

        return new TmapRouteResult(allCoordinates, totalDistance, totalTime, optionName);
    }

    private int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
