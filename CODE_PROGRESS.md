# SafetyRoad — 코드 진행 현황

---

## 백엔드

### 전체 흐름

```
클라이언트 요청
    ↓
Controller (라우팅/파라미터 검증)
    ↓
Service (비즈니스 로직)
    ↓
Repository (PostGIS 공간 쿼리)
    ↓
PostgreSQL / 외부 API (TMap, 공공데이터)
```

---

### 1. 데이터 수집 — OpenDataService

안전시설 데이터를 외부에서 가져와 DB에 저장.

**CCTV 동기화 (syncCctv)**
- 경기데이터드림 API `/CCTV` 페이지 단위(1,000건) 반복 호출
- `REFINE_WGS84_LAT`, `REFINE_WGS84_LOGT` 좌표 파싱
- 성남시 범위 필터 후 `SafetyFacility` 엔티티 저장
- 기존 데이터 delete 후 재삽입 방식

**보안등 CSV 임포트 (syncSecurityLightsFromCsv)**
- 공공데이터포털 전국 보안등 CSV 파싱 (API 차단으로 전환)
- 인코딩: EUC-KR
- 컬럼: `[0]위치명, [2]도로명주소, [3]지번주소, [4]위도, [5]경도`
- 성남시 좌표 범위 필터 → 8,971건 추출 (기존 API 89건)

**성남시 좌표 범위**
```
위도: 37.33 ~ 37.52
경도: 127.02 ~ 127.20
(초기 MAX_LNG=127.16 → 은행동·금광동·양지동 누락 → 127.20으로 수정)
```

---

### 2. 위험구역 계산 — DangerZoneService

성남시 전역을 격자로 나눠 각 셀의 안전점수를 계산해 DB에 저장.

**격자 사양**
```
위도 0.0027° × 경도 0.0034° (약 300m × 300m)
```

**계산 흐름**
```
1. 기존 위험구역 전체 삭제
2. 성남시 범위를 격자 단위로 순회
3. 각 셀 내 CCTV + 보안등 개수 조회 (countWithinBounds)
4. 시설 개수 → 안전점수 환산
5. 주변 1칸 확장 범위에도 시설이 없으면 제외 (외곽 빈 구역 배제)
6. 나머지 DangerZone 엔티티 저장
```

**안전점수 환산**
```
시설 0개  → 10점
     1~2  → 30~40점
     3~5  → 40~61점
     6~10 → 61~81점
    11개+ → 최대 100점
```

**리스크 레벨**
```
HIGH   :  0 ~ 30점  (빨강)
MEDIUM : 31 ~ 60점  (주황)
LOW    : 61 ~ 100점 (초록)
```

보안등 추가 후 1,090개 → 1,588개 위험구역

---

### 3. 경로 탐색 — TmapClient + RouteService

**TmapClient**
- TMap 보행자 경로 API 호출 (3가지 옵션 순차 요청)
  ```
  옵션 0  → 추천
  옵션 4  → 대로우선
  옵션 10 → 최단
  ```
- GeoJSON LineString에서 좌표 배열 추출
- totalDistance(m), totalTime(초)는 첫 Point 피처 properties에서 파싱

**RouteService — 안전점수 계산**
```
안전점수 = 위험구역점수(60%) + 시설밀도점수(40%)
```

- **위험구역 점수**: 경로 좌표 10개마다 1개 샘플링 → 해당 좌표가 속한 DangerZone의 safetyScore 평균 (구역 없으면 50점)
- **시설밀도 점수**: 경로 좌표 10개마다 1개 샘플링 → 반경 100m(≈0.001°) 내 시설 개수 → 점수 환산
- `recommended=true`: 안전점수 최고 경로 1개 자동 선택

---

### 4. 시설 조회 — SafetyFacilityRepository

**findByTypeWithinBounds** — 타입 지정
```sql
SELECT * FROM safety_facility
WHERE facility_type = :type
  AND ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))
LIMIT :limit
```

**findWithinBounds** — 타입 미지정, 라운드로빈
```sql
SELECT * FROM (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY facility_type ORDER BY id) AS rn
  FROM safety_facility
  WHERE ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))
) sub ORDER BY rn, facility_type LIMIT :limit
```
CCTV와 SECURITY_LIGHT가 limit 내에서 균등하게 섞여 반환됨

**countWithinBounds** — 격자 내 시설 수 (위험구역 계산용)

**countWithinRadius** — 반경 내 시설 수 (경로 점수 계산용)

---

### 5. API 엔드포인트 목록

| Controller | 메서드 | 경로 | 설명 |
|---|---|---|---|
| SafetyFacilityController | GET | /api/facilities | 뷰포트 내 시설 조회 |
| DangerZoneController | GET | /api/danger-zones | 전체 위험구역 조회 |
| DangerZoneController | POST | /api/danger-zones/recalculate | 위험구역 재계산 |
| RouteController | GET | /api/routes/safe | 안전경로 3가지 탐색 |
| SearchController | GET | /api/search/pois | TMap POI 검색 |
| OpenDataController | POST | /api/opendata/sync/cctv | CCTV 동기화 |
| OpenDataController | POST | /api/opendata/sync/security-light-csv | 보안등 CSV 임포트 |
| HealthCheckController | GET | /api/health | 헬스체크 |

---

---

## 프론트엔드

### 전체 흐름

```
App.js (상태 총괄)
├── KakaoMap.js           지도 인스턴스 생성 → onMapReady 콜백
├── SearchBar.js          TMap POI 검색 → 출발지/목적지 선택
├── RouteOverlay.js       선택 경로 폴리라인 렌더링
├── DangerZoneOverlay.js  위험구역 폴리곤 레이어
├── FacilityOverlay.js    CCTV/보안등 마커 레이어
├── RouteResult.js        경로 카드 목록
└── SOSButton.js          긴급전화 버튼 (껍데기)
```

---

### 1. 상태 관리 — App.js

모든 핵심 상태가 App.js에 집중, 하위 컴포넌트에 props로 전달.

| 상태 | 설명 |
|---|---|
| map | Kakao 지도 인스턴스 |
| origin / destination | 출발지/목적지 좌표 {lat, lng} |
| selectedRouteId | 선택된 경로 UUID |
| showDangerZones | 위험구역 레이어 토글 (기본: true) |
| showFacilities | 시설 레이어 토글 (기본: false) |
| bounds | 현재 지도 뷰포트 bbox |
| zoom | 현재 카카오맵 줌 레벨 |

**뷰포트 감지 (디바운스 500ms)**
```js
kakao.maps.event.addListener(map, 'bounds_changed', update);
kakao.maps.event.addListener(map, 'zoom_changed', update);
// 500ms 후 bounds, zoom 상태 갱신
```

---

### 2. 위험구역 레이어 — DangerZoneOverlay.js

**표시 범위 결정**
```
경로 검색 결과 있음 → 선택된 경로 bbox ± 0.02° 내 위험구역만 표시
경로 검색 전       → origin 반경 2km 내 위험구역만 표시 (Haversine 공식)
```

**폴리곤 렌더링**
- HIGH: 빨강 (fillOpacity 0.35)
- MEDIUM: 주황 (0.25)
- LOW: 초록 (0.15)

**팝업 (CustomOverlay)**
- 클릭 시 팝업: 리스크 레벨 배지, 안전점수, 시설 수, 점수 게이지 바
- 닫기: 팝업 내 ✕ 버튼(`window.__closeDzPopup`) 또는 지도 클릭

---

### 3. 시설 마커 레이어 — FacilityOverlay.js

**표시 조건**
```
visible=true AND zoom ≤ 5 (Kakao 레벨, 1=최대줌인 / 14=최대줌아웃)
```
너무 넓은 범위에서 수천 개 마커 동시 렌더링 방지 목적

**데이터 조회**
```js
// CCTV와 SECURITY_LIGHT 별도 병렬 요청 (limit 충돌 방지)
Promise.all([
  fetch('/api/facilities?...&type=CCTV'),
  fetch('/api/facilities?...&type=SECURITY_LIGHT'),
])
```
단일 요청 시 limit=1000 내에서 한 타입이 다른 타입을 밀어내는 문제 해결

**마커**
- CCTV: 파란 점 8px (`#3b82f6`)
- 보안등: 노란 점 8px (`#eab308`)
- 클릭 시 이름 툴팁 (`window.__showFacilityTooltip`)
- bounds/zoom 변경 시 전체 제거 후 재렌더링

---

### 4. 경로 결과 카드 — RouteResult.js

```
[🛡️ 가장 안전한 경로로 안내] 버튼
─────────────────────────────
[추천] 추천 경로
  15분  1.2km
  안전 ▓▓▓▓▓▓▓▓▓░░  85점
─────────────────────────────
대로우선 경로
  13분  1.1km
  보통 ▓▓▓▓▓▓░░░░░  62점
─────────────────────────────
최단 경로
  10분  0.9km
  위험 ▓▓░░░░░░░░░  35점
```

**점수 레벨 (safetyUtils.js)**
```
71 이상 → safe   (초록)
41~70  → medium (주황)
40 이하 → danger (빨강)
```

---

### 5. 데이터 흐름 요약

```
[지도 뷰포트 변경]
  → bounds/zoom 갱신 (500ms 디바운스)
  → FacilityOverlay: zoom ≤ 5 이면 API 재조회 + 마커 재렌더링

[경로 검색 버튼 클릭]
  → GET /api/routes/safe
  → RouteResponse[] 반환
  → selectedRouteId = recommended 경로 자동 선택
  → RouteOverlay: 폴리라인 렌더링
  → DangerZoneOverlay: 선택 경로 bbox 기준 위험구역 필터

[경로 카드 클릭]
  → selectedRouteId 변경
  → RouteOverlay: 굵기/색상 변경
  → DangerZoneOverlay: 해당 경로 bbox 기준으로 위험구역 재필터
```

---

## 데이터 현황

| 종류 | 건수 |
|---|---|
| CCTV | 2,958건 |
| 보안등 | 8,971건 |
| 위험구역 그리드 | 1,588개 |

---

## 남은 작업

| 우선순위 | 항목 |
|---|---|
| 높음 | console.log 디버그 코드 제거 |
| 중간 | 모바일 반응형 레이아웃 |
| 중간 | 위험구역 API 백엔드 bbox 필터링 (현재 프론트 필터) |
| 낮음 | SOS 버튼 실제 기능 구현 |
| 낮음 | 서비스 배포 설정 |
