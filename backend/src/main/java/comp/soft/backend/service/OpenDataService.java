package comp.soft.backend.service;

import comp.soft.backend.entity.SafetyFacility;
import comp.soft.backend.repository.SafetyFacilityRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;

@Service
public class OpenDataService {

    private static final Logger log = LoggerFactory.getLogger(OpenDataService.class);
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String BASE_URL = "https://openapi.gg.go.kr";
    private static final int PAGE_SIZE = 1000;
    private static final double MIN_LAT = 37.33;
    private static final double MAX_LAT = 37.52;
    private static final double MIN_LNG = 127.02;
    private static final double MAX_LNG = 127.20;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SafetyFacilityRepository facilityRepository;

    @Value("${opendata.cctv-key}")
    private String cctvKey;

    @Value("${opendata.security-light-location-key}")
    private String securityLightLocationKey;

    @Value("${opendata.security-light-stats-key}")
    private String securityLightStatsKey;

    @Value("${opendata.streetlight-stats-key}")
    private String streetlightStatsKey;

    @Value("${opendata.police-box-key}")
    private String policeBoxKey;

    @Value("${opendata.district-police-key}")
    private String districtPoliceKey;

    public OpenDataService(SafetyFacilityRepository facilityRepository) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.facilityRepository = facilityRepository;
    }

    public Map<String, Integer> syncAll() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("CCTV", syncCctv());
        result.put("SECURITY_LIGHT", syncSecurityLights());
        result.put("CSV_SECURITY_LIGHT", syncSecurityLightsFromCsv());
        result.put("POLICE_BOX", syncPoliceBoxes());
        result.put("DISTRICT_POLICE", syncDistrictPolice());
        return result;
    }

    public int syncCctv() {
        log.info("CCTV 데이터 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;

        while (true) {
            Map response = fetchApi("/CCTV", cctvKey, page, PAGE_SIZE);
            if (response == null) break;

            List<Map<String, Object>> rows = extractRows(response, "CCTV");
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, Object> row : rows) {
                String lat = Objects.toString(row.get("REFINE_WGS84_LAT"), "");
                String lng = Objects.toString(row.get("REFINE_WGS84_LOGT"), "");
                String addr = Objects.toString(row.get("REFINE_ROADNM_ADDR"), "");

                if (lat.isEmpty() || lng.isEmpty()) continue;
                if (!isInSeongnam(lat, lng)) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("CCTV");
                f.setName(Objects.toString(row.get("INSTL_PUPRS_DIV_NM"), "CCTV"));
                f.setAddress(addr.isEmpty() ? Objects.toString(row.get("REFINE_LOTNO_ADDR"), "") : addr);
                f.setLocation(geometryFactory.createPoint(
                        new Coordinate(Double.parseDouble(lng), Double.parseDouble(lat))
                ));
                f.setDataSource("경기데이터드림");
                facilities.add(f);
            }

            int totalCount = extractTotalCount(response, "CCTV");
            if (page * PAGE_SIZE >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("CCTV", "경기데이터드림");
            facilityRepository.saveAll(facilities);
        }

        log.info("CCTV 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    public int syncSecurityLights() {
        log.info("보안등 데이터 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;

        while (true) {
            Map response = fetchApi("/SECRTLGT", securityLightLocationKey, page, PAGE_SIZE);
            if (response == null) break;

            List<Map<String, Object>> rows = extractRows(response, "SECRTLGT");
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, Object> row : rows) {
                String lat = Objects.toString(row.get("REFINE_WGS84_LAT"), "");
                String lng = Objects.toString(row.get("REFINE_WGS84_LOGT"), "");
                String addr = Objects.toString(row.get("REFINE_ROADNM_ADDR"), "");

                if (lat.isEmpty() || lng.isEmpty()) continue;
                if (!isInSeongnam(lat, lng)) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("SECURITY_LIGHT");
                f.setName(Objects.toString(row.get("SECRT_LGT_DVSN_NM"), "보안등"));
                f.setAddress(addr.isEmpty() ? Objects.toString(row.get("REFINE_LOTNO_ADDR"), "") : addr);
                f.setLocation(geometryFactory.createPoint(
                        new Coordinate(Double.parseDouble(lng), Double.parseDouble(lat))
                ));
                f.setDataSource("경기데이터드림");
                facilities.add(f);
            }

            int totalCount = extractTotalCount(response, "SECRTLGT");
            if (page * PAGE_SIZE >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("SECURITY_LIGHT", "경기데이터드림");
            facilityRepository.saveAll(facilities);
        }

        log.info("보안등 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    public int syncPoliceBoxes() {
        log.info("치안센터 데이터 동기화 시작");
        return syncPoliceType("/PoliceCenter", "PoliceCenter", "POLICE_BOX", policeBoxKey, "치안센터");
    }

    public int syncDistrictPolice() {
        log.info("지구대 데이터 동기화 시작");
        return syncPoliceType("/PoliceStation", "PoliceStation", "DISTRICT_POLICE", districtPoliceKey, "지구대");
    }

    private int syncPoliceType(String apiPath, String rootKey, String facilityType, String apiKey, String defaultName) {
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;

        while (true) {
            Map response = fetchApi(apiPath, apiKey, page, PAGE_SIZE);
            if (response == null) break;

            List<Map<String, Object>> rows = extractRows(response, rootKey);
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, Object> row : rows) {
                String lat = Objects.toString(row.get("REFINE_WGS84_LAT"), "");
                String lng = Objects.toString(row.get("REFINE_WGS84_LOGT"), "");
                String addr = Objects.toString(row.get("REFINE_ROADNM_ADDR"), "");
                String name = Objects.toString(row.get("FACILITY_NM"), "");
                if (name.isEmpty()) name = Objects.toString(row.get("INSTT_NM"), defaultName);

                if (lat.isEmpty() || lng.isEmpty()) continue;
                if (!isInSeongnam(lat, lng)) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType(facilityType);
                f.setName(name);
                f.setAddress(addr.isEmpty() ? Objects.toString(row.get("REFINE_LOTNO_ADDR"), "") : addr);
                f.setLocation(geometryFactory.createPoint(
                        new Coordinate(Double.parseDouble(lng), Double.parseDouble(lat))
                ));
                f.setDataSource("경기데이터드림");
                facilities.add(f);
            }

            int totalCount = extractTotalCount(response, rootKey);
            if (page * PAGE_SIZE >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource(facilityType, "경기데이터드림");
            facilityRepository.saveAll(facilities);
        }

        log.info("{} 동기화 완료: {}건", facilityType, facilities.size());
        return facilities.size();
    }

    @SuppressWarnings("unchecked")
    private Map fetchApi(String path, String key, int page, int size) {
        try {
            String url = String.format("%s%s?KEY=%s&Type=json&pIndex=%d&pSize=%d",
                    BASE_URL, path, key, page, size);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json, text/html, */*")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body == null || body.trim().startsWith("<")) {
                log.error("API HTML 응답 (차단): {}", path);
                return null;
            }

            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("API 호출 실패: {} - {}", path, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map response, String rootKey) {
        try {
            List<Map<String, Object>> root = (List<Map<String, Object>>) response.get(rootKey);
            if (root == null || root.size() < 2) return null;
            return (List<Map<String, Object>>) root.get(1).get("row");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private int extractTotalCount(Map response, String rootKey) {
        try {
            List<Map<String, Object>> root = (List<Map<String, Object>>) response.get(rootKey);
            List<Map<String, Object>> head = (List<Map<String, Object>>) root.get(0).get("head");
            return (int) head.get(0).get("list_total_count");
        } catch (Exception e) {
            return 0;
        }
    }

    public int syncSecurityLightsFromCsv() {
        log.info("보안등 CSV 데이터 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("data/seongnam_security_light.csv")) {
            if (is == null) {
                log.warn("CSV 파일을 찾을 수 없음: data/seongnam_security_light.csv");
                return 0;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("CP949")));

            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] cols = line.split(",", -1);
                if (cols.length < 6) continue;

                String name    = cols[0].trim();
                String roadAddr = cols[2].trim();
                String lotAddr  = cols[3].trim();
                String latStr  = cols[4].trim();
                String lngStr  = cols[5].trim();

                if (latStr.isEmpty() || lngStr.isEmpty()) continue;

                double lat, lng;
                try {
                    lat = Double.parseDouble(latStr);
                    lng = Double.parseDouble(lngStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (!isInSeongnam(latStr, lngStr)) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("SECURITY_LIGHT");
                f.setName(name.isEmpty() ? "보안등" : name);
                f.setAddress(roadAddr.isEmpty() ? lotAddr : roadAddr);
                f.setLocation(geometryFactory.createPoint(new Coordinate(lng, lat)));
                f.setDataSource("CSV");
                facilities.add(f);
            }
        } catch (Exception e) {
            log.error("CSV 읽기 실패: {}", e.getMessage());
            return 0;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("SECURITY_LIGHT", "CSV");
            facilityRepository.saveAll(facilities);
        }

        log.info("보안등 CSV 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    private boolean isInSeongnam(String latStr, String lngStr) {
        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);
            return lat >= MIN_LAT && lat <= MAX_LAT && lng >= MIN_LNG && lng <= MAX_LNG;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
