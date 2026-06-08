# SafetyRoad 프로젝트 현황 리포트 (상세 버전)

> 작성일: 2026-06-08  
> 대상 독자: 이 프로젝트를 처음 접하는 AI 에이전트 또는 신규 팀원  
> 목적: 코드 컨텍스트 없이도 프로젝트의 전체 역사·상태·버그를 파악할 수 있도록 TMI 수준으로 기술

---

## 1. 프로젝트 개요

**SafetyRoad**는 성남시 분당구를 주 대상으로 하는 **AI 기반 안전 도보 경로 추천 웹 애플리케이션**이다.

### 핵심 아이디어
- 도보 경로는 Tmap API가 여러 옵션(최단·추천 등)으로 제공
- 각 경로가 통과하는 구간에 CCTV·보안등·경찰시설이 얼마나 있는지를 분석해 **안전 점수** 산출
- 가장 점수가 높은 경로를 "추천 안전경로"로 제시

### 서비스 범위
- **지역**: 경기도 성남시 (분당구·수정구·중원구)
- **시설 유형**: CCTV, 보안등(가로등 포함), 경찰/치안시설(지구대·파출소·치안센터)
- 소방서는 경찰시설 API에서 혼재되어 있으나 **의도적으로 제외** 처리

---

## 2. 팀 구성 및 역할 분담

| 이름 | 역할 | GitHub |
|------|------|--------|
| 김령균 | 주 개발자 (풀스택, 메인 유지보수) | - |
| 이시우 | API 키 발급 및 관리 담당 | - |
| 이동희 | 백엔드 개발 참여 | ldongheedev |

- **GitHub 원격 저장소**: `https://github.com/ldongheedev/safety_road.git`
  - 대표 계정이 이동희의 계정인 이유: 팀 저장소 소유자가 ldongheedev
- **현재 브랜치**: `main` 단일 브랜치 운용 (feature 브랜치 없음)
- **git user**: kimrg

### 팀 협업 시 발생한 이슈
- 동시 push로 인한 merge conflict 발생 이력 있음 (App.js import 영역 충돌)
- 팀원이 RouteResult.js에 안전 점수 섹션을 추가했다가 → 메인 개발자가 삭제 요청 → 다시 팀원이 재추가하는 형태로 코드 혼선 있었음
- **현재 상태**: RouteResult.js에 안전 점수 바가 표시되어 있음 (팀원 재추가 버전이 남아있음)

---

## 3. 기술 스택 (선택 배경 포함)

### 백엔드

| 항목 | 내용 | 비고 |
|------|------|------|
| 언어 | Java 25 | 원래 Java 21로 개발하던 중 Java 25로 업그레이드. Eclipse Adoptium JDK 25.0.3.9-hotspot 사용 |
| 프레임워크 | Spring Boot 4.0.6 | |
| 빌드 도구 | Maven (mvnw) | 초기에 Gradle로 시작했다가 Maven으로 교체. `backend/.gradle/` 디렉토리가 남아있었으나 정리 완료 |
| DB | PostgreSQL + PostGIS | DB명: `safetyroad_db`, 포트: `5432`, 사용자: `postgres`, 비밀번호: `fm136` |
| ORM | 없음 (순수 JdbcTemplate) | **중요**: 초기에는 JPA 사용을 시도했으나 `application.yml`에 JPA/Hibernate 설정이 남아있었음. Maven pom.xml에는 JPA 의존성이 없어서 실제로는 JdbcTemplate만 사용. 잘못된 JPA 설정 블록은 이후 정리됨 |
| 공간 DB | PostGIS | ST_Within, ST_DWithin, ST_Distance, ST_MakeEnvelope, ST_Point, ST_SetSRID 등 사용 |
| HTTP 클라이언트 | java.net.http.HttpClient (JDK 내장) | WebClient 빈이 설정파일에 존재하지만 실제로는 HttpClient를 직접 사용 |

### 프론트엔드

| 항목 | 내용 |
|------|------|
| 언어 | JavaScript (React 18, CRA — Create React App) |
| 지도 | Kakao Maps JavaScript API |
| HTTP | fetch 내장 API (주로) + Axios (일부) |
| CSS | 글로벌 CSS (`global.css`) — 별도 CSS 프레임워크 없음 |
| 빌드 | CRA 기본 (`npm start` 개발, `npm run build` 프로덕션) |
| 포트 | 3000 (개발서버), 백엔드 프록시 → 8080 |

---

## 4. 파일 구조 (현재 상태)

```
C:\safety road\
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/comp/soft/backend/
│   │   │   │   ├── BackendApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── CorsConfig.java              ← CORS 허용 설정
│   │   │   │   │   └── WebClientConfig.java         ← WebClient 빈 (실제로는 HttpClient 사용)
│   │   │   │   ├── controller/
│   │   │   │   │   ├── DangerZoneController.java    ← GET /api/danger-zones, POST /recalculate
│   │   │   │   │   ├── HealthCheckController.java   ← GET /api/health
│   │   │   │   │   ├── OpenDataController.java      ← POST /api/opendata/sync/*
│   │   │   │   │   ├── RouteController.java         ← GET /api/routes
│   │   │   │   │   ├── SafetyFacilityController.java← GET /api/facilities
│   │   │   │   │   └── SearchController.java        ← GET /api/search (카카오 프록시)
│   │   │   │   ├── dto/
│   │   │   │   │   ├── RouteResponse.java
│   │   │   │   │   └── TmapRouteResult.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── DangerZone.java
│   │   │   │   │   └── SafetyFacility.java
│   │   │   │   ├── external/
│   │   │   │   │   └── TmapClient.java              ← Tmap 도보 경로 API 호출
│   │   │   │   ├── repository/
│   │   │   │   │   ├── DangerZoneRepository.java
│   │   │   │   │   └── SafetyFacilityRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── DangerZoneService.java       ← 격자 기반 위험구역 계산
│   │   │   │       ├── OpenDataService.java         ← 공공데이터 API 동기화 (핵심)
│   │   │   │       └── RouteService.java            ← 안전 점수 계산
│   │   │   └── resources/
│   │   │       ├── application.yml                  ← gitignore 처리됨! (API 키 포함)
│   │   │       └── data/
│   │   │           └── seongnam_security_light.csv  ← gitignore 처리됨 (*.csv)
│   │   └── test/
│   │       └── java/comp/soft/backend/
│   │           └── BackendApplicationTests.java
│   └── pom.xml
├── frontend/
│   ├── public/
│   │   └── index.html
│   └── src/
│       ├── App.js                                   ← 최상위 컴포넌트
│       ├── index.js
│       ├── api/
│       │   └── index.js                             ← Axios 기반 API 헬퍼
│       ├── components/
│       │   ├── KakaoMap.js                          ← 지도 초기화
│       │   ├── SearchBar.js                         ← 출발지/도착지 검색
│       │   ├── SOSButton.js                         ← 긴급 SOS 버튼
│       │   ├── Map/
│       │   │   ├── FacilityOverlay.js               ← 시설 마커 렌더링
│       │   │   └── RouteOverlay.js                  ← 경로 폴리라인 렌더링
│       │   └── Route/
│       │       └── RouteResult.js                   ← 경로 결과 카드
│       ├── hooks/
│       │   └── useRoute.js                          ← 경로 검색 상태 관리
│       ├── styles/
│       │   └── global.css
│       └── utils/
│           └── safetyUtils.js                       ← 점수 → 레벨 변환 유틸
├── .gitignore
├── CLAUDE.md                                        ← AI 에이전트 지시사항
├── start-backend.bat                                ← 백엔드 시작 스크립트 (JAVA_HOME 설정 포함)
└── PROJECT_STATUS_REPORT.md                        ← 이 파일
```

### 삭제된 파일 (이전에 존재했다가 정리됨)
- `frontend/src/components/TmapView.js` — 사용되지 않던 구 Tmap 뷰어
- `frontend/src/pages/HomePage.js` — 사용되지 않던 홈페이지
- `frontend/src/components/Map/DangerZoneOverlay.js` — 위험구역 폴리곤 렌더러 (성능 문제로 제거)
- `frontend/src/hooks/useDangerZones.js` — DangerZoneOverlay와 함께 제거
- `.env.example` — 예시 파일 (정리)
- `frontend/.env.example` — 예시 파일 (정리)
- `backend/src/main/resources/application.properties.example` — 예시 파일 (정리)
- `backend/.gradle/` — Maven 전환 후 남은 Gradle 캐시 (정리)

---

## 5. DB 스키마

> ⚠️ 별도의 마이그레이션 파일(Flyway/Liquibase 등)이 없음. JPA auto-ddl도 없음. 현재 코드 분석을 통해 역추적한 스키마임.

### `safety_facility` 테이블

```sql
CREATE TABLE safety_facility (
    id            BIGSERIAL PRIMARY KEY,
    facility_type VARCHAR(50)   NOT NULL,  -- 'CCTV', 'SECURITY_LIGHT', 'POLICE'
    name          VARCHAR(255),
    address       VARCHAR(500),
    location      GEOMETRY(Point, 4326),   -- PostGIS 공간 컬럼 (경도, 위도)
    data_source   VARCHAR(100),            -- 데이터 출처 구분자
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

-- 권장 인덱스 (코드에는 명시 없음, 성능상 필요)
CREATE INDEX idx_safety_facility_location ON safety_facility USING GIST(location);
CREATE INDEX idx_safety_facility_type ON safety_facility(facility_type);
```

**data_source 종류**: `경기데이터드림`, `경기데이터드림_치안`, `CSV`, `공공데이터포털_조도`, `공공데이터포털_성남CCTV`

### `danger_zone` 테이블

```sql
CREATE TABLE danger_zone (
    id             BIGSERIAL PRIMARY KEY,
    min_lat        DOUBLE PRECISION,
    min_lng        DOUBLE PRECISION,
    max_lat        DOUBLE PRECISION,
    max_lng        DOUBLE PRECISION,
    safety_score   DECIMAL(5,2),
    facility_count INTEGER,
    risk_level     VARCHAR(20)   -- 'HIGH'(≤30), 'MEDIUM'(≤60), 'LOW'(>60)
);
```

위험구역은 성남시를 **격자(grid)**로 나눠 각 격자의 시설 수를 기반으로 안전 점수를 산출한 결과물. 격자 크기: 위도 0.0027°, 경도 0.0034° (약 300m × 300m).

---

## 6. API 키 목록 및 현재 상태

`application.yml`에 하드코딩된 기본값 형태로 저장됨. 환경변수로 오버라이드 가능.

| 설정 키 | 환경변수 | 출처 | 용도 |
|---------|---------|------|------|
| `tmap.app-key` | `TMAP_APP_KEY` | 이시우 발급 | Tmap 도보 경로 |
| `kakao.rest-api-key` | `KAKAO_REST_API_KEY` | 이시우 발급 | 카카오 장소 검색 프록시 |
| `opendata.cctv-key` | `DATA_CCTV_KEY` | 이시우 발급 | 경기데이터드림 CCTV |
| `opendata.security-light-location-key` | `DATA_SECURITY_LIGHT_LOCATION_KEY` | 이시우 발급 | 경기데이터드림 보안등 위치 |
| `opendata.security-light-stats-key` | `DATA_SECURITY_LIGHT_STATS_KEY` | 이시우 발급 | 경기데이터드림 보안등 통계 (현재 미사용) |
| `opendata.streetlight-stats-key` | `DATA_STREETLIGHT_STATS_KEY` | 이시우 발급 | 경기데이터드림 가로등 통계 (현재 미사용) |
| `opendata.police-key` | `DATA_POLICE_KEY` | 이시우 발급 | 경기데이터드림 소방/경찰 XML API |
| `opendata.illumination-key` | `DATA_ILLUMINATION_KEY` | 이시우 발급 | 공공데이터포털 조도측정 보안등 |

> `application.yml`은 `.gitignore`에 등록되어 있어 GitHub에 올라가지 않음. 팀원은 로컬에 직접 파일을 만들어야 함.

---

## 7. 공공데이터 동기화 상세

### 7-1. CCTV (경기데이터드림)
- **API**: `https://openapi.gg.go.kr/CCTV?KEY=&Type=json&pIndex=&pSize=`
- **파싱 방식**: JSON, 경기도 전체 데이터에서 성남시 좌표 범위 필터링
- **저장 타입**: `CCTV`, `data_source = '경기데이터드림'`
- **특이사항**: 은행동·금광동·상대원동(중원구 일부) 데이터가 없었음 → 원인은 경기도 전체 CCTV API에 해당 지역 데이터가 누락된 것

### 7-2. 성남시 생활안전 CCTV (공공데이터포털)
- **API**: `https://api.odcloud.kr/api/15147955/v1/uddi:6134ce56-...`
- **파싱 방식**: JSON, 한글 컬럼명 (`위도`, `경도`, `도로명주소`, `지번주소`)
- **저장 타입**: `CCTV`, `data_source = '공공데이터포털_성남CCTV'`
- **건수**: 약 3,026건
- **추가 목적**: 경기데이터드림 CCTV에 빠진 중원구 지역 보완
- **버그**: `illuminationKey`를 API 키로 사용 중 → 전용 키가 있다면 수정 필요

### 7-3. 보안등 위치 (경기데이터드림)
- **API**: `https://openapi.gg.go.kr/SECRTLGT?KEY=&Type=json&pIndex=&pSize=`
- **파싱 방식**: JSON
- **저장 타입**: `SECURITY_LIGHT`, `data_source = '경기데이터드림'`

### 7-4. 보안등 (CSV)
- **파일**: `backend/src/main/resources/data/seongnam_security_light.csv`
- **인코딩**: CP949 (EUC-KR)
- **컬럼**: `[0]이름, [2]도로명주소, [3]지번주소, [4]위도, [5]경도`
- **저장 타입**: `SECURITY_LIGHT`, `data_source = 'CSV'`
- **주의**: `.gitignore` (`*.csv`)에 의해 GitHub에 없음. 신규 환경에서 `syncAll()` 호출 시 파일 없으면 0건으로 처리 (오류는 없음)

### 7-5. 조도측정 보안등 (공공데이터포털)
- **API**: `https://api.odcloud.kr/api/15110584/v1/uddi:8ccdb2d7-...`
- **파싱 방식**: JSON, 한글 컬럼명 (`위도(LATITUDE)`, `경도(LONGITUDE)`)
- **저장 타입**: `SECURITY_LIGHT`, `data_source = '공공데이터포털_조도'`
- **건수**: 약 3,310건

### 7-6. 소방/경찰/지구대/치안센터 (경기데이터드림)
- **API**: `https://openapi.gg.go.kr/FiresttnPolcsttnM?KEY=&Type=xml&pIndex=&pSize=`
- **파싱 방식**: XML DOM 파싱 (`<row>` 태그 반복)
- **저장 타입**: `POLICE`, `data_source = '경기데이터드림_치안'`
- **건수**: 약 35건 (성남시 필터 후)
- **소방서 제외**: `FACLT_DIV_NM`에 "소방서" 포함 시 skip
- **배경**: 초기에는 `POLICE_BOX`(파출소), `DISTRICT_POLICE`(지구대) 두 타입으로 분리했으나, 두 API 모두 키값이 없어 0건이었음. FiresttnPolcsttnM 단일 API로 통합하고 타입을 `POLICE`로 일원화

#### XML 파싱 시 주의사항
- API는 태그명을 소문자로 반환 (`sigun_nm`) 하지만 코드에서는 `.toUpperCase()` 처리해서 `SIGUN_NM`으로 접근
- 경기데이터드림 API는 `User-Agent` 헤더 없이 호출하면 HTML 오류 페이지 반환 → 모든 요청에 Mozilla User-Agent 헤더 필수

---

## 8. 백엔드 API 상세

### GET `/api/routes`
```
?startLat=37.xxx&startLng=127.xxx&endLat=37.xxx&endLng=127.xxx
```
- Tmap 도보 경로를 여러 옵션으로 조회
- 각 경로에 대해 `calculateSafetyScore()` 실행
- 가장 안전 점수 높은 경로에 `recommended: true` 설정
- 반환: `[{ routeId, coordinates: [[lat,lng],...], totalDistance, totalTime, safetyScore, recommended, searchOption }]`

### GET `/api/facilities`
```
?lat1=&lng1=&lat2=&lng2=&limit=200&type=CCTV
```
- `type` 파라미터 있으면: `findByTypeWithinBounds()` → 중심점 기준 가까운 순 정렬 (ORDER BY ST_Distance)
- `type` 없으면: `findWithinBounds()` → ROW_NUMBER()로 타입별 균등 분배
- `limit` 최대 1000 제한 (safeLimit)
- 반환: `[{ id, facilityType, lat, lng }]`

### GET `/api/danger-zones`
- 전체 `danger_zone` 테이블 반환
- 각 zone에 격자 좌표 배열 포함 (`bounds`)
- 프론트엔드에서는 현재 미사용 (DangerZoneOverlay 제거됨)

### POST `/api/danger-zones/recalculate`
- 성남시 전체를 격자로 나눠 각 격자의 시설 수 계산
- 기존 위험구역 전체 삭제 후 재생성
- 시설이 전혀 없고 인접 격자에도 없으면 그 격자는 저장 안 함 (공백 지역 제외)

### GET `/api/search`
```
?query=성남시청
```
- 카카오 REST API 장소 검색을 백엔드에서 프록시
- API 키를 프론트에 노출하지 않기 위한 구조

### GET `/api/health`
- 단순 200 OK 응답

---

## 9. 프론트엔드 상세 동작

### App.js 구조
```
App
├── KakaoMap (지도)
├── side-panel (좌측 패널)
│   ├── 출발지/도착지 선택 버튼 (지도 클릭 모드 토글)
│   ├── SearchBar (카카오 장소 검색)
│   ├── 경로 검색 버튼
│   ├── RouteResult (경로 카드 목록)
│   └── 시설 ON/OFF 토글 버튼 + 범례
├── RouteOverlay (경로 폴리라인)
├── FacilityOverlay (시설 마커)
└── SOSButton (우하단 FAB)
```

### FacilityOverlay.js 동작 흐름
1. `map`, `visible`, `bounds`, `zoom` 중 하나라도 변경 → useEffect 재실행
2. `!zoom || zoom > 5` → 기존 마커 전부 제거 후 아무것도 안 함 (줌 조건 미충족)
3. bounds와 zoom이 유효하면 CCTV, SECURITY_LIGHT, POLICE 3개 타입 **병렬 fetch**
4. 각 시설마다 `CustomOverlay`(원형 dot)를 Kakao Maps에 추가
5. bounds/zoom은 `bounds_changed` + `zoom_changed` 이벤트에서 **500ms 디바운스** 후 업데이트
6. 마커 클릭 시 `window.__showFacilityTooltip()` 글로벌 함수 호출 → CustomOverlay 툴팁 표시

**줌 레벨 null 버그 해결 배경**: 지도 초기화 직후 `zoom` state가 null인 상태에서 `null > 5`는 JavaScript에서 false → 조건이 통과되어 마커가 렌더링되는 버그가 있었음. `!zoom ||` 조건 추가로 해결.

### Kakao 줌 레벨 체계
- 레벨 1: 최대 줌인 (건물 단위)
- 레벨 5: 약 동네 단위 (마커 표시 한계)
- 레벨 14: 최대 줌아웃 (국가 단위)

### 마커 스펙
| 타입 | 색상 | 크기 | zIndex |
|------|------|------|--------|
| CCTV | `#3b82f6` (파랑) | 10px | 10 |
| SECURITY_LIGHT | `#eab308` (노랑) | 8px | 10 |
| POLICE | `#ef4444` (빨강) | 12px | 15 (위에 표시) |

**마커 색상 통일화 배경**: 초기에는 CCTV, 보안등, 경찰 각각 다른 색이었음. 한때 전부 빨간색으로 통일 요청이 있었으나 이후 현재의 3색 체계로 확정됨.

### RouteOverlay.js
- 선택된 경로: 진한 색 폴리라인
- 비선택 경로: 흐린 색 폴리라인
- `getLevelColor(safetyScore)` 사용하여 점수에 따라 색상 결정
- **제거된 기능**: `dangerZones` prop, `findDangerSegments()`, `isPointInPolygon()` — 성능 문제로 모두 제거

### 안전 점수 표시 (RouteResult.js)
- 팀원(이동희)이 추가한 안전 점수 바가 현재 남아있음
- 71점 이상: 안전(초록), 41~70점: 보통(주황), 40점 이하: 위험(빨강)
- CSS: `.route-safety-section`, `.route-safety-bar`, `.route-safety-bar-fill--*`

### SOSButton.js
- 우하단 고정 빨간 FAB (`position: fixed; bottom: 24px; right: 24px`)
- 클릭 → 모달 오픈 → 5초 카운트다운 원형 프로그레스
- 카운트다운 중 "지금 신고" 클릭 또는 5초 경과 → `tel:112` 링크로 전화 연결
- "취소" 클릭 → 모달 닫기

---

## 10. 안전 점수 계산 알고리즘 상세

**파일**: `RouteService.java`

```
최종 안전 점수 = 위험구역 점수 × 0.6 + 시설 밀도 점수 × 0.4
```

### 위험구역 점수 (`calcDangerZoneScore`)
- 경로 좌표 배열을 10개 간격으로 샘플링
- 각 샘플 좌표에 대해 `dangerZoneRepository.findByPoint(lat, lng)` 호출
- 해당 좌표를 포함하는 격자의 `safety_score` 평균을 사용
- 격자에 해당 없으면 50.0점 기본값

### 시설 밀도 점수 (`calcFacilityDensityScore`)
- 경로 좌표 배열을 10개 간격으로 샘플링
- 각 샘플 좌표 반경 0.001° (약 100m) 내 시설 수 카운트
- 반경 0.003° (약 300m) 내 경찰시설 수 카운트 → 5배 가중치
- 합계 → 아래 점수 테이블 적용:

```
count=0  → 0점
count=1  → 30점
count=2  → 40점
count=3  → 47점
count=4  → 54점
count=5  → 61점
count=6  → 65점
count=7  → 69점
count=8  → 73점
count=9  → 77점
count=10 → 81점
count>10 → 81 + (count-10) × 1.5점 (최대 100점)
```

> ⚠️ **현재 버그**: `countPoliceWithinRadius()`가 `POLICE_BOX`/`DISTRICT_POLICE`를 조회하므로 경찰 가중치가 항상 0. DB에는 `POLICE` 타입으로 저장됨.

---

## 11. 위험구역 격자 계산 상세

**파일**: `DangerZoneService.java`

- 성남시 범위: 위도 37.33~37.52, 경도 127.04~127.16
- 격자 크기: 위도 0.0027°, 경도 0.0034°
- 각 격자마다: `countWithinBounds()` + `countPoliceWithinBounds() × 5`
- 시설 0건이고 인접 격자에도 없으면 해당 격자는 저장 안 함
- `recalculate()` 호출 시 기존 전체 삭제 후 재생성

> ⚠️ `countPoliceWithinBounds()`도 `POLICE_BOX`/`DISTRICT_POLICE` 조회 → 버그 1과 동일 문제

---

## 12. 알려진 버그 (중요도 순)

### 🔴 버그 1: 경찰시설 타입 불일치 (안전점수 오류)

**파일**: `SafetyFacilityRepository.java`, `countPoliceWithinRadius()` (77~81번째 줄), `countPoliceWithinBounds()` (84~90번째 줄)

**현상**: 경로 안전 점수 계산 시 경찰시설 가중치가 항상 0으로 계산됨. 위험구역 재계산 시에도 경찰시설 카운트가 항상 0.

**원인**: DB에는 경찰시설이 타입 `POLICE`로 저장되어 있으나, 두 카운트 메서드의 SQL 쿼리에 구 타입명 `POLICE_BOX`, `DISTRICT_POLICE`가 하드코딩되어 있음.

**히스토리**: 초기에 파출소는 `POLICE_BOX`, 지구대는 `DISTRICT_POLICE`로 분리된 API와 타입이 있었음. 각 API 키가 비어있어 항상 0건이었고, FiresttnPolcsttnM 단일 API로 통합하면서 타입을 `POLICE`로 일원화했음. 그러나 Repository의 카운트 메서드는 구 타입명이 그대로 남아버린 것.

**수정 방법**:
```java
// SafetyFacilityRepository.java 77번째 줄 부근
// 현재 (잘못됨)
"WHERE facility_type IN ('POLICE_BOX', 'DISTRICT_POLICE') ..."
// 수정
"WHERE facility_type = 'POLICE' ..."
```
두 메서드 모두 동일하게 수정 필요.

**영향**: 
- `RouteService.calcFacilityDensityScore()` → 경찰 가중치 항상 0 → 안전점수 과소평가
- `DangerZoneService.recalculate()` → 위험구역 격자 점수가 경찰시설 기여분 없이 계산됨

---

### 🔴 버그 2: `syncSeongnamCctv()`에서 잘못된 API 키 사용

**파일**: `OpenDataService.java`, `syncSeongnamCctv()` (286번째 줄)

**현상**: 성남시 생활안전 CCTV API 호출 시 조도측정 API 키(`illuminationKey`)를 사용 중.

```java
// 현재 코드
String url = String.format(
    "https://api.odcloud.kr/api/15147955/v1/...?page=%d&perPage=%d&serviceKey=%s",
    page, perPage, illuminationKey);  // ← 잘못된 키
```

**현재 작동 여부**: 두 API 모두 공공데이터포털 계정의 같은 키를 공유하는 경우라면 우연히 작동할 수 있음. 키가 다르면 인증 오류.

**수정 방법**: `application.yml`에 `seongnam-cctv-key` 항목 추가 + `OpenDataService`에 `@Value` 추가 후 사용.

---

### 🟡 버그 3: `countPoliceWithinBounds()` 관련 위험구역 재계산 오류

**파일**: `SafetyFacilityRepository.java` (84~90번째 줄)

버그 1의 연장선. `DangerZoneService`의 `recalculate()`에서 호출되는 `countPoliceWithinBounds()`도 구 타입명 사용. 위험구역 재계산 결과에 경찰시설 기여가 반영되지 않음.

---

### 🟡 문제 4: DB 테이블 초기화 스크립트 없음

**현상**: JPA auto-ddl 없고, Flyway/Liquibase 없고, 별도 SQL 스크립트도 없음. 신규 환경에서 클론 후 백엔드 실행 시 테이블이 없어서 모든 쿼리가 실패함.

**현재 해결책**: 없음. 팀 내부에서 수동으로 DDL을 실행하는 것으로 추정.

**권장 조치**: `schema.sql` 파일 생성 (Spring Boot가 시작 시 자동 실행하도록 설정 가능).

---

### 🟡 문제 5: CSV 보안등 파일 미포함

**현상**: `seongnam_security_light.csv` 파일이 `.gitignore`의 `*.csv` 패턴에 의해 Git에 포함되지 않음. 신규 환경 클론 시 해당 파일 없음.

**영향**: `syncAll()` 또는 `syncSecurityLightsFromCsv()` 호출 시 0건 반환 (오류는 없음, 경고 로그만 출력).

**권장 조치**: 파일이 필요하다면 `resources/data/*.csv`를 gitignore 예외 처리하거나, 해당 데이터를 공공 API로 대체.

---

### 🟡 문제 6: `security-light-stats-key`, `streetlight-stats-key` 미사용

**현상**: `application.yml`에 두 키가 있지만 `OpenDataService`에 관련 `@Value`나 메서드가 없음.

**영향**: 없음 (데드 설정값, 백엔드 정상 동작에 영향 없음).

---

### 🟢 이미 해결된 버그들 (참고용)

| 버그 | 원인 | 해결 |
|------|------|------|
| FacilityOverlay 초기 null 줌 → 마커 불필요 렌더링 | `null > 5 === false`로 조건 통과 | `!zoom \|\| zoom > MAX_KAKAO_LEVEL` |
| SafetyFacilityController type 파라미터 무시 | 컨트롤러에 파라미터 없었음 | `@RequestParam(required=false) String type` 추가 |
| XML 파싱 태그 대소문자 불일치 | API는 소문자, 코드는 대문자 접근 | `.toUpperCase()` 처리 |
| 경기데이터드림 API User-Agent 차단 | 봇으로 인식 → HTML 오류 반환 | Mozilla User-Agent 헤더 추가 |
| POLICE_BOX/DISTRICT_POLICE API 항상 0건 | API 키 없음 | 두 메서드 삭제, FiresttnPolcsttnM으로 통합 |
| DangerZoneOverlay 렌더링으로 렉 발생 | 수백 개 폴리곤 동시 렌더링 | DangerZoneOverlay 컴포넌트 완전 제거 |
| 중원구/은행동 CCTV 없음 | 경기데이터드림 데이터 누락 | 성남시 생활안전 CCTV (공공데이터포털) 추가 |
| 지역별 마커 수 불균형 | ORDER BY 없이 LIMIT → 임의 500건 | `ORDER BY ST_Distance(위치, 중심점)` 추가 |
| 백엔드 시작 실패 | `police-box-key`, `district-police-key` placeholder 미해결 | 두 키 항목 완전 제거 |
| JPA/Hibernate 설정 충돌 | Maven에 JPA 없는데 yml에 JPA 설정 잔존 | application.yml의 JPA 설정 블록 삭제 |
| App.js merge conflict | 팀원 동시 push | `<<<<<<<`, `=======`, `>>>>>>>` 마커 수동 제거 |

---

## 13. .gitignore 현황

```gitignore
backend/build/
frontend/node_modules/
frontend/dist/
frontend/build/

*.env
.env.*
!.env.example       ← 실제 .env.example 파일은 삭제됨. 이 줄이 불필요하게 남아있음

*.sql
*.csv
*.zip
*보고서*.md
*.pdf
TEAM_BRIEFING.md
frontend/.env
frontend/.env.*
!frontend/.env.example   ← 동일, 불필요한 예외 규칙

backend/src/main/resources/application.yml
backend/src/main/resources/application-*.yml

.idea/
.vscode/
*.iml
.gradle/
*.yml
*.yaml
```

> ⚠️ `*.yml`과 `*.yaml`이 전체 무시됨. 이는 `application.yml`을 숨기기 위한 의도이나, 다른 YAML 파일도 모두 차단됨. 더 좁은 패턴(`backend/src/main/resources/*.yml`)으로 교체하는 것이 나을 수 있음.

---

## 14. application.yml 최종 상태 (현재)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/safetyroad_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:fm136}
    driver-class-name: org.postgresql.Driver

tmap:
  app-key: ${TMAP_APP_KEY:S5jlB4eKpe9JFTom4T3ye1KWm88A2ck667teasl8}

kakao:
  rest-api-key: ${KAKAO_REST_API_KEY:6a31669752a108094baf920035833d89}

opendata:
  security-light-stats-key: ${DATA_SECURITY_LIGHT_STATS_KEY:7d9420298e63457bafea6a8b18615c4e}
  streetlight-stats-key: ${DATA_STREETLIGHT_STATS_KEY:49a97b1bb14a438e838461d57b0fc61b}
  cctv-key: ${DATA_CCTV_KEY:79f26d1b8d004013b64708bb4f9922fa}
  security-light-location-key: ${DATA_SECURITY_LIGHT_LOCATION_KEY:1724c503a2f9451dbf70957e1719c5d3}
  police-key: ${DATA_POLICE_KEY:776898df5764406c87ef3c832e901520}
  illumination-key: ${DATA_ILLUMINATION_KEY:21ac5ca181b72fb9393a1f620d2e83f95b357aca984e4ce553d381d9bbe19c31}
```

삭제된 설정 (이유):
- `spring.jpa.*` 전체 블록 → JPA 의존성 없음, 충돌 가능성 있어 제거
- `opendata.police-box-key`, `opendata.district-police-key` → 해당 API/메서드 삭제됨

---

## 15. 현재 구현 완료 여부

### ✅ 완전 구현
- Tmap 도보 경로 조회 (복수 경로)
- 경로 안전 점수 계산 (버그 있으나 기능은 동작)
- 추천 경로 표시 및 강조
- 카카오 맵 렌더링
- 출발지/도착지 카카오 장소 검색
- 지도 클릭 → 출발지/도착지 선택
- 시설 마커 표시 (CCTV/보안등/경찰, 3색)
- 시설 ON/OFF 토글 버튼
- 줌 기반 마커 표시 조건 (레벨 ≤ 5)
- 마커 툴팁 (클릭 시)
- SOS 긴급 버튼 (카운트다운 + 112 연결)
- 공공데이터 동기화 API (6종)
- 위험구역 격자 계산 API (DB는 존재하나 프론트에서 미사용)
- 경로 결과 카드 (거리/시간/안전점수 바)

### 🔧 구현됐으나 버그 있음
- 경찰시설 가중 안전점수 (버그 1)
- 위험구역 경찰시설 반영 (버그 1 연동)
- 성남시 CCTV 동기화 (버그 2 — 키 불일치 가능성)

### ❌ 미구현
- DB 초기화 스크립트 (신규 환경 세팅 불편)
- 사용자 인증/로그인
- 경로 저장/이력
- 배포 환경 (현재 로컬 전용)
- 위험구역 시각화 (DangerZoneOverlay 제거됨, 재추가 여부 미정)

---

## 16. 환경 실행 방법

### 백엔드
```powershell
cd "C:\safety road\backend"
..\mvnw spring-boot:run
```
또는 `start-backend.bat` 실행 (JAVA_HOME: `C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot`).

### 프론트엔드
```powershell
cd "C:\safety road\frontend"
npm start
```

### 데이터 동기화 (최초 세팅)
```powershell
curl -X POST http://localhost:8080/api/opendata/sync/cctv
curl -X POST http://localhost:8080/api/opendata/sync/security-light
curl -X POST http://localhost:8080/api/opendata/sync/police
curl -X POST http://localhost:8080/api/opendata/sync/illumination-light
curl -X POST http://localhost:8080/api/opendata/sync/seongnam-cctv
curl -X POST http://localhost:8080/api/danger-zones/recalculate
```

---

## 17. 최근 git 커밋 이력

```
90c98dd chore: dead code 및 불필요한 파일 정리
1eba1cf feat: 성남시 CCTV/보안등 데이터 추가 및 시설 마커 개선
6354ae3 fix: 시설 마커 줌 체크 및 type 파라미터 처리 수정
c76fa3d fix: App.js 충돌 마커 제거
b06a68a refactor: 불필요한 코드 전체 정리
```

---

*이 문서는 2026-06-08 기준이며, 소스 코드를 직접 분석하여 작성됨.*  
*버그 1(경찰시설 타입 불일치)은 수정 전 상태로, 안전 점수가 부정확하게 계산되고 있음.*
