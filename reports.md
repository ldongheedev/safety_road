# SafeRoute 프로젝트 보고서

> 최종 업데이트: 2026-04-06
> 본 문서는 팀원이 AI와 함께 바로 개발에 착수할 수 있도록 프로젝트 전체 현황을 정리한 문서입니다.

---

## 1. 프로젝트 개요

**SafeRoute (안전 귀갓길)** — 성남시 분당구 대상, 야간 보행자를 위한 안전 경로 안내 서비스

### 핵심 기능
보안등, CCTV 등 공공시설물 데이터를 API로 수집하고, 이를 기반으로 **더 안전한 도보 경로를 추천**하는 것이 프로젝트의 핵심입니다.

### 기술 스택
| 구분 | 기술 |
|------|------|
| 백엔드 | Java 17+, Spring Boot 3, Gradle |
| DB | PostgreSQL + PostGIS (공간 데이터) |
| 프론트엔드 | React 18, Axios |
| 지도 | 카카오맵 SDK (프론트), TMap API (경로 탐색) |
| 외부 API | TMap (경로/POI 검색), 경기데이터드림 (CCTV/보안등) |

---

## 2. 프로젝트 구조

### 백엔드 (`backend/`)
```
src/main/java/com/saferoute/
├── SafeRouteApplication.java     # Spring Boot 메인
├── config/
│   ├── CorsConfig.java           # CORS 설정 (localhost:3000, 5173 허용)
│   └── WebClientConfig.java      # WebClient 빈 설정
├── controller/
│   ├── HealthCheckController.java # 헬스체크
│   ├── SearchController.java      # POI 검색 (TMap 연동)
│   ├── RouteController.java       # 안전 경로 API
│   ├── DangerZoneController.java  # 위험구역 조회/재계산
│   └── OpenDataController.java    # 공공데이터 동기화
├── dto/
│   ├── RouteResponse.java         # 경로 응답 DTO
│   └── TmapRouteResult.java       # TMap 응답 파싱 결과
├── entity/
│   ├── SafetyFacility.java        # 안전시설 엔티티 (CCTV, 보안등 등)
│   └── DangerZone.java            # 위험구역 엔티티 (격자 폴리곤)
├── external/
│   └── TmapClient.java            # TMap API 클라이언트
├── repository/
│   ├── SafetyFacilityRepository.java  # 시설 조회 (PostGIS 공간쿼리)
│   └── DangerZoneRepository.java      # 위험구역 조회
└── service/
    ├── RouteService.java          # 경로 비즈니스 로직
    ├── DangerZoneService.java     # 위험구역 계산 로직
    └── OpenDataService.java       # 공공데이터 수집 로직
```

### 프론트엔드 (`frontend/`)
```
src/
├── App.js                          # 메인 앱 (지도 + 검색 + 경로 + 위험구역 + SOS)
├── api/index.js                    # Axios 인스턴스 (baseURL: /api, proxy → :8080)
├── components/
│   ├── KakaoMap.js                 # 카카오맵 초기화
│   ├── SearchBar.js                # 출발지/목적지 검색 (TMap POI)
│   ├── SOSButton.js                # SOS 긴급 신고 버튼
│   ├── Map/
│   │   ├── RouteOverlay.js         # 경로 지도 오버레이 (폴리라인)
│   │   └── DangerZoneOverlay.js    # 위험구역 지도 오버레이 (폴리곤)
│   └── Route/
│       └── RouteResult.js          # 경로 결과 목록 카드
├── hooks/
│   ├── useRoute.js                 # 경로 검색 훅 (GET /api/routes/safe)
│   └── useDangerZones.js           # 위험구역 조회 훅 (GET /api/danger-zones)
├── styles/
│   └── global.css                  # 전역 스타일
└── pages/
    └── HomePage.js                 # (미사용)
```

---

## 3. DB 스키마

### safety_facility (안전시설)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| facility_type | VARCHAR(20) | CCTV, SECURITY_LIGHT 등 |
| name | VARCHAR(100) | 시설명 |
| address | VARCHAR(200) | 주소 |
| location | GEOMETRY(Point, 4326) | 좌표 (PostGIS) |
| data_source | VARCHAR(50) | 데이터 출처 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### danger_zone (위험구역)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| grid_polygon | GEOMETRY(Polygon, 4326) | 300m 격자 폴리곤 |
| safety_score | DECIMAL(5,2) | 안전 점수 (0~100, 높을수록 안전) |
| facility_count | INTEGER | 격자 내 시설 수 |
| risk_level | VARCHAR(10) | HIGH / MEDIUM / LOW |
| calculated_at | TIMESTAMP | |

---

## 4. API 스펙 (현재 구현됨)

### 4.1 POI 검색
```
GET /api/search/pois?keyword={검색어}
```
- TMap POI 검색 API 프록시
- 응답:
```json
[
  { "name": "서현역", "address": "경기 성남시 분당구...", "lat": 37.3837, "lng": 127.1264 }
]
```

### 4.2 안전 경로 검색
```
GET /api/routes/safe?startLat={}&startLng={}&endLat={}&endLng={}
```
- TMap 보행자 경로를 3가지 옵션(추천/대로우선/최단)으로 요청
- 응답:
```json
[
  {
    "routeId": "uuid",
    "coordinates": [[37.38, 127.12], [37.39, 127.13], ...],
    "totalDistance": 1200,
    "totalTime": 900,
    "safetyScore": null,       // ⚠️ 현재 미구현 (항상 null)
    "recommended": false,      // ⚠️ 현재 미구현 (항상 false)
    "searchOption": "추천"
  }
]
```

### 4.3 위험구역 조회
```
GET /api/danger-zones
```
- 모든 위험구역 격자 반환
- 응답:
```json
[
  {
    "id": 1,
    "safetyScore": 10.0,
    "facilityCount": 0,
    "riskLevel": "HIGH",
    "bounds": [[37.33, 127.04], [37.33, 127.0434], [37.3327, 127.0434], [37.3327, 127.04], [37.33, 127.04]]
  }
]
```

### 4.4 위험구역 재계산
```
POST /api/danger-zones/recalculate
```
- 모든 격자에 대해 시설 밀도 기반 안전점수 재계산
- 응답: `{ "message": "위험구역 재계산 완료", "totalZones": 285 }`

### 4.5 공공데이터 동기화
```
POST /api/opendata/sync         # 전체 동기화 (CCTV + 보안등)
POST /api/opendata/sync/cctv    # CCTV만
POST /api/opendata/sync/security-light  # 보안등만
```
- 경기데이터드림 API에서 데이터 수집 → DB 저장
- 성남시 분당구 범위(lat 37.33~37.42, lng 127.04~127.16)만 필터링

---

## 5. 현재 구현 상태

| 기능 | 상태 | 파일 |
|------|------|------|
| 카카오맵 표시 | ✅ 완료 | `KakaoMap.js` |
| 출발지/목적지 검색 | ✅ 완료 | `SearchBar.js` → `SearchController.java` |
| 현재 위치 출발지 설정 | ✅ 완료 | `SearchBar.js` (Geolocation API) |
| TMap 도보 경로 3개 탐색 | ✅ 완료 | `TmapClient.java` → `RouteService.java` |
| 경로 지도 표시 | ✅ 완료 | `RouteOverlay.js` |
| 경로 결과 카드 UI | ✅ 완료 | `RouteResult.js` |
| 공공데이터 수집 (CCTV, 보안등) | ✅ 완료 | `OpenDataService.java` |
| 위험구역 격자 계산 | ✅ 완료 | `DangerZoneService.java` |
| 위험구역 지도 표시 (ON/OFF) | ✅ 완료 | `DangerZoneOverlay.js` |
| SOS 버튼 | ⚠️ 껍데기 | `SOSButton.js` (tel:112 링크만) |
| **경로별 안전도 스코어링** | ❌ 미구현 | `RouteService.java` (safetyScore=null) |
| **안전 경로 추천** | ❌ 미구현 | `RouteService.java` (recommended=false) |

---

## 6. 핵심 미구현 기능: 안전 경로 스코어링

현재 가장 큰 빈 구멍입니다. 데이터(시설 위치)와 경로(TMap)는 다 있지만, **경로가 얼마나 안전한지 계산하는 로직이 없습니다.**

### 구현해야 할 로직 (RouteService.java)
1. TMap에서 경로 좌표 리스트를 받아옴 (현재 완료)
2. 각 경로의 좌표들에 대해:
   - 해당 좌표가 속한 **위험구역(danger_zone)의 risk_level** 확인 → PostGIS `ST_Intersects` 활용
   - 경로 주변 반경 내 **안전시설(safety_facility) 개수** 집계 → PostGIS `ST_DWithin` 활용
3. 위 정보를 조합해 **경로별 안전 점수(safetyScore)** 산출
4. 가장 안전한 경로에 `isRecommended = true` 설정

### 이미 준비된 PostGIS 쿼리들
- `DangerZoneRepository.findByPoint(lat, lng)` — 특정 좌표가 속한 위험구역 조회
- `SafetyFacilityRepository.findWithinRadius(lat, lng, radius)` — 반경 내 시설 조회
- `SafetyFacilityRepository.countWithinBounds(lat1, lng1, lat2, lng2)` — 범위 내 시설 수

---

## 7. 개발 로드맵

### 공통 (먼저)
- [ ] API 응답 스펙 합의 (safetyScore, recommended 필드 형식 확정)

### 백엔드 개발자
- [ ] **1순위: 안전 경로 스코어링 구현** (`RouteService.java`)
  - TMap 경로 좌표 → 위험구역 통과 여부 판별 → 안전시설 밀도 계산 → 점수 산출
  - `RouteResponse`의 `safetyScore`와 `isRecommended`를 실제 값으로 채우기
- [ ] **2순위: 공공데이터 확장**
  - 가로등, 편의점, 경찰서, 비상벨 등 추가 시설 수집
  - `OpenDataService`에 새 API 연동 추가
  - `SafetyFacility.facilityType`에 새 타입 추가
- [ ] **3순위: 자동화**
  - `@Scheduled`로 공공데이터 주기적 동기화
  - 위험구역 자동 재계산

### 프론트엔드 개발자
- [ ] **1순위: 안전 경로 UI**
  - 경로 카드에 안전 점수 시각화 (프로그레스 바, 색상 등)
  - 추천 경로 강조 표시 (뱃지, 하이라이트)
  - 지도 위 경로 색상 차별화 (안전=초록, 위험=빨강)
  - 백엔드 완성 전까지 mock 데이터로 개발 가능:
    ```json
    { "safetyScore": 78.5, "recommended": true }
    ```
- [ ] **2순위: 위험구역 상세 UI**
  - 위험구역 클릭 시 팝업 (시설 개수, 위험 등급 상세)
  - 경로 위 위험구간 하이라이트
- [ ] **3순위: 사용성 개선**
  - 모바일 반응형 레이아웃
  - "가장 안전한 경로로 안내" 원클릭 버튼
  - 로딩/에러 상태 개선

---

## 8. 환경 설정

### 실행 방법
```bash
# 백엔드 (포트 8080)
cd backend
./gradlew bootRun

# 프론트엔드 (포트 3000, proxy → 8080)
cd frontend
npm install
npm start
```

### 설정 파일
- `backend/src/main/resources/application.yml` — DB, API 키 설정
- `frontend/public/index.html` — 카카오맵 JS 키
- `frontend/package.json` — `"proxy": "http://localhost:8080"`

### DB 설정
- PostgreSQL + PostGIS 확장 필요
- DB명: `saferoute`
- 초기 데이터: `backend/src/main/resources/db/data.sql`
- JPA ddl-auto: `update` (엔티티 기반 자동 스키마 생성)

### API 키 (application.yml에 설정됨)
| 항목 | 용도 |
|------|------|
| tmap.app-key | TMap 경로/POI 검색 |
| kakao.rest-api-key | 카카오 REST API |
| opendata.cctv-key | 경기데이터드림 CCTV |
| opendata.security-light-location-key | 경기데이터드림 보안등 |
| opendata.security-light-stats-key | 보안등 통계 |
| opendata.streetlight-stats-key | 가로등 통계 |

### 대상 지역 범위 (분당구)
- 위도: 37.33 ~ 37.42
- 경도: 127.04 ~ 127.16
- 격자 크기: 약 300m (위도 0.0027, 경도 0.0034)

---

## 9. 주요 데이터 흐름

```
[사용자 검색] → SearchBar → GET /api/search/pois → TMap POI API → 결과 표시
                                                         
[경로 검색]   → useRoute → GET /api/routes/safe → TmapClient (3가지 옵션)
                                                     ↓
                                              RouteService
                                              (TODO: 안전도 계산)
                                                     ↓
                                              RouteResponse 반환
                                                     ↓
                                     RouteResult (카드) + RouteOverlay (지도)

[위험구역]    → useDangerZones → GET /api/danger-zones → DangerZoneRepository
                                                              ↓
                                                    DangerZoneOverlay (지도)

[공공데이터]  → POST /api/opendata/sync → 경기데이터드림 API → safety_facility 테이블
              → POST /api/danger-zones/recalculate → 격자별 시설 밀도 계산 → danger_zone 테이블
```

---

## 10. 개발 시 참고사항

- CORS는 `localhost:3000`, `localhost:5173`만 허용 중 (`CorsConfig.java`)
- 프론트에서 `/api/*` 요청은 `package.json`의 proxy 설정으로 백엔드로 전달됨
- PostGIS 공간 쿼리를 적극 활용하고 있으므로, PostgreSQL에 PostGIS 확장이 반드시 설치되어야 함
- `data.sql`에 분당구 주요 지점의 CCTV/보안등/가로등 초기 데이터가 포함됨
- 위험구역 계산은 시설이 0개이고 주변에도 시설이 없는 외곽 빈 격자는 제외됨
