package comp.soft.backend.service;

import comp.soft.backend.entity.SafetyFacility;
import comp.soft.backend.repository.SafetyFacilityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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

    @Value("${opendata.police-key}")
    private String policeKey;

    @Value("${opendata.illumination-key}")
    private String illuminationKey;

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
        result.put("POLICE", syncPolice());
        result.put("ILLUMINATION_LIGHT", syncIlluminationLights());
        result.put("SEONGNAM_CCTV", syncSeongnamCctv());
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
                f.setLatitude(Double.parseDouble(lat));
                f.setLongitude(Double.parseDouble(lng));
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
                f.setLatitude(Double.parseDouble(lat));
                f.setLongitude(Double.parseDouble(lng));
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

    public int syncPolice() {
        log.info("소방/경찰/지구대/치안센터 데이터 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = String.format("%s/FiresttnPolcsttnM?KEY=%s&Type=xml&pIndex=%d&pSize=%d",
                    BASE_URL, policeKey, page, PAGE_SIZE);
            String xml = fetchXmlApi(url);
            if (xml == null) break;

            List<Map<String, String>> rows = parseXmlRows(xml);
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, String> row : rows) {
                String sigun = row.getOrDefault("SIGUN_NM", "");
                if (!sigun.contains("성남시")) continue;

                String lat = row.getOrDefault("REFINE_WGS84_LAT", "");
                String lng = row.getOrDefault("REFINE_WGS84_LOGT", "");
                if (lat.isEmpty() || lng.isEmpty()) continue;
                if (!isInSeongnam(lat, lng)) continue;

                String facltDiv = row.getOrDefault("FACLT_DIV_NM", "");
                String name = row.getOrDefault("INST_NM", "경찰시설");
                String addr = row.getOrDefault("REFINE_ROADNM_ADDR", "");
                if (addr.isEmpty()) addr = row.getOrDefault("REFINE_LOTNO_ADDR", "");

                // 소방서 제외
                if (facltDiv.contains("소방서")) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("POLICE");
                f.setName(name);
                f.setAddress(addr);
                f.setLatitude(Double.parseDouble(lat));
                f.setLongitude(Double.parseDouble(lng));
                f.setDataSource("경기데이터드림_치안");
                facilities.add(f);
            }

            int totalCount = extractXmlTotalCount(xml);
            if (totalCount == 0 || page * PAGE_SIZE >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("POLICE", "경기데이터드림_치안");
            facilityRepository.saveAll(facilities);
        }

        log.info("소방/경찰 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    @SuppressWarnings("unchecked")
    public int syncIlluminationLights() {
        log.info("조도측정 보안등 데이터 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;
        int perPage = 1000;

        while (true) {
            String url = String.format(
                    "https://api.odcloud.kr/api/15110584/v1/uddi:8ccdb2d7-f990-4334-855f-d75ae55fa92a?page=%d&perPage=%d&serviceKey=%s",
                    page, perPage, illuminationKey);
            Map response = fetchApi(url, perPage, page);
            if (response == null) break;

            List<Map<String, Object>> rows;
            try {
                rows = (List<Map<String, Object>>) response.get("data");
            } catch (Exception e) {
                break;
            }
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, Object> row : rows) {
                String latStr = Objects.toString(row.get("위도(LATITUDE)"), "").trim();
                String lngStr = Objects.toString(row.get("경도(LONGITUDE)"), "").trim();
                if (latStr.isEmpty() || lngStr.isEmpty()) continue;
                if (!isInSeongnam(latStr, lngStr)) continue;

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("SECURITY_LIGHT");
                f.setName("가로등");
                f.setAddress("");
                f.setLatitude(Double.parseDouble(latStr));
                f.setLongitude(Double.parseDouble(lngStr));
                f.setDataSource("공공데이터포털_조도");
                facilities.add(f);
            }

            int totalCount = ((Number) response.getOrDefault("totalCount", 0)).intValue();
            if (page * perPage >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("SECURITY_LIGHT", "공공데이터포털_조도");
            facilityRepository.saveAll(facilities);
        }

        log.info("조도측정 보안등 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    @SuppressWarnings("unchecked")
    public int syncSeongnamCctv() {
        log.info("성남시 생활안전 CCTV 동기화 시작");
        List<SafetyFacility> facilities = new ArrayList<>();
        int page = 1;
        int perPage = 1000;

        while (true) {
            String url = String.format(
                    "https://api.odcloud.kr/api/15147955/v1/uddi:6134ce56-07cc-45d9-b3d9-ed6fc76d9d4c?page=%d&perPage=%d&serviceKey=%s",
                    page, perPage, illuminationKey);
            Map response = fetchApi(url, perPage, page);
            if (response == null) break;

            List<Map<String, Object>> rows;
            try {
                rows = (List<Map<String, Object>>) response.get("data");
            } catch (Exception e) {
                break;
            }
            if (rows == null || rows.isEmpty()) break;

            for (Map<String, Object> row : rows) {
                String latStr = Objects.toString(row.get("위도"), "").trim();
                String lngStr = Objects.toString(row.get("경도"), "").trim();
                if (latStr.isEmpty() || lngStr.isEmpty()) continue;
                if (!isInSeongnam(latStr, lngStr)) continue;

                String addr = Objects.toString(row.get("도로명주소"), "");
                if (addr.isEmpty()) addr = Objects.toString(row.get("지번주소"), "");

                SafetyFacility f = new SafetyFacility();
                f.setFacilityType("CCTV");
                f.setName("생활안전CCTV");
                f.setAddress(addr);
                f.setLatitude(Double.parseDouble(latStr));
                f.setLongitude(Double.parseDouble(lngStr));
                f.setDataSource("공공데이터포털_성남CCTV");
                facilities.add(f);
            }

            int totalCount = ((Number) response.getOrDefault("totalCount", 0)).intValue();
            if (page * perPage >= totalCount) break;
            page++;
        }

        if (!facilities.isEmpty()) {
            facilityRepository.deleteByFacilityTypeAndDataSource("CCTV", "공공데이터포털_성남CCTV");
            facilityRepository.saveAll(facilities);
        }

        log.info("성남시 CCTV 동기화 완료: {}건", facilities.size());
        return facilities.size();
    }

    @SuppressWarnings("unchecked")
    private Map fetchApi(String fullUrl, int size, int page) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json, */*")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body == null || body.trim().startsWith("<")) {
                log.error("API HTML 응답 (차단): {}", fullUrl);
                return null;
            }
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("API 호출 실패: {} - {}", fullUrl, e.getMessage());
            return null;
        }
    }

    private String fetchXmlApi(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/xml, text/xml, */*")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body == null || body.isBlank()) return null;
            return body;
        } catch (Exception e) {
            log.error("XML API 호출 실패: {} - {}", url, e.getMessage());
            return null;
        }
    }

    private List<Map<String, String>> parseXmlRows(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            NodeList rowNodes = doc.getElementsByTagName("row");
            List<Map<String, String>> rows = new ArrayList<>();

            for (int i = 0; i < rowNodes.getLength(); i++) {
                Element rowEl = (Element) rowNodes.item(i);
                NodeList children = rowEl.getChildNodes();
                Map<String, String> row = new LinkedHashMap<>();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j) instanceof Element child) {
                        row.put(child.getTagName().toUpperCase(), child.getTextContent().trim());
                    }
                }
                if (!row.isEmpty()) rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private int extractXmlTotalCount(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList list = doc.getElementsByTagName("list_total_count");
            if (list.getLength() > 0) {
                return Integer.parseInt(list.item(0).getTextContent().trim());
            }
        } catch (Exception e) {
            log.error("XML totalCount 파싱 실패: {}", e.getMessage());
        }
        return 0;
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
                f.setLatitude(lat);
                f.setLongitude(lng);
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
