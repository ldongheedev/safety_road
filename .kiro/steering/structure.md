# 프로젝트 구조 (Structure)

모노레포: 루트에 `backend/`(Spring Boot), `frontend/`(React)가 있다.

## 디렉터리

```
safety road/
├── backend/
│   └── src/main/java/comp/soft/backend/
│       ├── config/      CorsConfig, WebClientConfig
│       ├── controller/  DangerZone, HealthCheck, OpenData, Route, SafetyFacility, Search
│       ├── dto/         RouteResponse, TmapRouteResult
│       ├── entity/      DangerZone, SafetyFacility (POJO + Lombok, JPA 아님)
│       ├── external/    TmapClient (도보 경로 호출, HttpClient)
│       ├── repository/  DangerZoneRepository, SafetyFacilityRepository (JdbcTemplate)
│       └── service/     DangerZoneService, OpenDataService, RouteService
│   └── src/main/resources/
│       ├── application.yml          (gitignore, API 키 포함)
│       └── data/seongnam_security_light.csv  (gitignore: *.csv)
└── frontend/src/
    ├── api/index.js          공용 axios 인스턴스 (baseURL '/api')
    ├── components/           KakaoMap, SearchBar, SOSButton, Map/*, Route/*
    ├── hooks/useRoute.js
    ├── utils/safetyUtils.js  안전 점수 → 레벨/색상/라벨
    └── styles/global.css
```

## REST 엔드포인트 (실제 코드 기준)

| 메서드 | 경로 | 파라미터 / 비고 |
|--------|------|------------------|
| GET | `/api/routes/safe` | `startLat, startLng, endLat, endLng` (double, 필수). 한국 좌표 범위 검증(lat 33~43, lng 124~132) |
| GET | `/api/search/pois` | `keyword` (String). **Tmap POI 검색**(apis.openapi.sk.com/tmap/pois), WebClient 사용 |
| GET | `/api/health` | `{ status: "ok", timestamp }` |
| GET | `/api/facilities` | `lat1, lng1, lat2, lng2` (필수), `limit`(기본 200), `type`(선택) |
| GET | `/api/danger-zones` | 전체 위험구역 반환 |
| POST | `/api/danger-zones/recalculate` | 전체 재계산 |
| POST | `/api/opendata/sync` | 전체 동기화(syncAll) |
| POST | `/api/opendata/sync/cctv` | syncCctv |
| POST | `/api/opendata/sync/security-light` | syncSecurityLights |
| POST | `/api/opendata/sync/police` | syncPolice |
| POST | `/api/opendata/sync/illumination-light` | syncIlluminationLights |
| POST | `/api/opendata/sync/seongnam-cctv` | syncSeongnamCctv |

`OpenDataService.syncAll()` 호출 순서: CCTV → SECURITY_LIGHT → CSV_SECURITY_LIGHT → POLICE → ILLUMINATION_LIGHT → SEONGNAM_CCTV. `syncSecurityLightsFromCsv()`는 `/sync`에서만 호출되며 개별 엔드포인트가 없다.

## CORS (CorsConfig)
- 허용 Origin: `http://localhost:3000`(CRA), `http://localhost:5173`(Vite, 현재 미사용이나 포함)
- 허용 메서드: GET, POST, PUT, DELETE, OPTIONS
- `allowCredentials(true)`, allowedHeaders 미지정(Spring 기본 `*`)

## DB 스키마 (수동 관리)

자동 마이그레이션/auto-ddl 없음. DDL은 `psql -f init.sql`(+`data.sql`)로 수동 실행한다. 운용 DB는 루트의 `safetyroad_dump_*.sql` 덤프 복원으로 추정. (init.sql/data.sql 소스는 현재 삭제되어 `backend/build/resources/main/db/`에 빌드 아티팩트만 남아 있음 — 필요 시 복구 권장)

### safety_facility
```sql
CREATE TABLE IF NOT EXISTS safety_facility (
    id            BIGSERIAL PRIMARY KEY,
    facility_type VARCHAR(20)  NOT NULL,   -- 'CCTV', 'SECURITY_LIGHT', 'POLICE'
    name          VARCHAR(100),
    address       VARCHAR(200),
    location      GEOMETRY(Point, 4326) NOT NULL,
    data_source   VARCHAR(50),
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_facility_location ON safety_facility USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_facility_type     ON safety_facility (facility_type);
```

### danger_zone
```sql
CREATE TABLE IF NOT EXISTS danger_zone (
    id            BIGSERIAL PRIMARY KEY,
    grid_polygon  GEOMETRY(Polygon, 4326) NOT NULL,
    safety_score  DECIMAL(5,2) NOT NULL,
    facility_count INTEGER DEFAULT 0,
    risk_level    VARCHAR(10),             -- 'HIGH'/'MEDIUM'/'LOW'
    calculated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_danger_zone_polygon ON danger_zone USING GIST (grid_polygon);
CREATE INDEX IF NOT EXISTS idx_danger_zone_risk    ON danger_zone (risk_level);
```

> `danger_zone`은 `grid_polygon`(Polygon) 단일 지오메트리로 저장한다. min/max lat·lng 컬럼은 **없다**. 코드에서는 `ST_YMin/ST_XMin/ST_YMax/ST_XMax`로 bounds를 파생한다.

> `data.sql` 시드에는 `STREETLIGHT`, `CONVENIENCE_STORE` 타입이 있으나 현재 코드는 `CCTV`/`SECURITY_LIGHT`/`POLICE`만 처리한다. 시드와 코드가 불일치하므로 타입 기준은 코드를 따른다.

## 신규 환경 세팅 시 주의
- `application.yml`, `seongnam_security_light.csv`, `*.sql` 덤프는 gitignore라 클론에 없다. 별도로 받아야 한다.
- 테이블이 없으면 모든 쿼리가 실패하므로 DDL을 먼저 실행해야 한다.
