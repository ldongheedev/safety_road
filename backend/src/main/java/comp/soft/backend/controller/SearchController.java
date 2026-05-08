package comp.soft.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final WebClient webClient;
    private final String tmapAppKey;

    public SearchController(WebClient.Builder webClientBuilder,
                            @Value("${tmap.app-key}") String tmapAppKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://apis.openapi.sk.com")
                .build();
        this.tmapAppKey = tmapAppKey;
    }

    @GetMapping("/pois")
    public List<Map<String, Object>> searchPois(@RequestParam String keyword) {
        Map response = webClient.get()
                .uri(uri -> uri
                        .path("/tmap/pois")
                        .queryParam("version", 1)
                        .queryParam("searchKeyword", keyword)
                        .queryParam("count", 5)
                        .build())
                .header("appKey", tmapAppKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return Collections.emptyList();

        Map searchPoiInfo = (Map) response.get("searchPoiInfo");
        if (searchPoiInfo == null) return Collections.emptyList();

        Map pois = (Map) searchPoiInfo.get("pois");
        if (pois == null) return Collections.emptyList();

        List<Map> poiList = (List<Map>) pois.get("poi");
        if (poiList == null) return Collections.emptyList();

        return poiList.stream().map(poi -> {
            String name = (String) poi.get("name");
            String addr = buildAddress(poi);
            String noorLat = (String) poi.get("noorLat");
            String noorLon = (String) poi.get("noorLon");
            String frontLat = (String) poi.get("frontLat");
            String frontLon = (String) poi.get("frontLon");

            double lat = (noorLat != null && !noorLat.isEmpty() && !noorLat.equals("0"))
                    ? Double.parseDouble(noorLat)
                    : Double.parseDouble(frontLat);
            double lng = (noorLon != null && !noorLon.isEmpty() && !noorLon.equals("0"))
                    ? Double.parseDouble(noorLon)
                    : Double.parseDouble(frontLon);

            return Map.<String, Object>of(
                    "name", name,
                    "address", addr,
                    "lat", lat,
                    "lng", lng
            );
        }).toList();
    }

    private String buildAddress(Map poi) {
        String upper = (String) poi.getOrDefault("upperAddrName", "");
        String middle = (String) poi.getOrDefault("middleAddrName", "");
        String lower = (String) poi.getOrDefault("lowerAddrName", "");
        String detail = (String) poi.getOrDefault("detailAddrName", "");
        return (upper + " " + middle + " " + lower + " " + detail).trim();
    }
}
