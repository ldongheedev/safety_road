# 백엔드 작업 보고서 (2026-04-06)

> 본 문서는 `reports.md`를 기반으로 오늘 진행된 백엔드 작업 내용을 정리한 문서입니다.
> 프론트엔드 개발자는 `reports.md`와 본 문서를 함께 참고하여 개발을 시작하세요.

---

## 변경 사항 요약

### 1. 프로젝트 명칭 변경
| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 프로젝트명 | SafeRoute | SafetyRoad |
| Java 패키지 | `com.saferoute.*` | `com.safetyroad.*` |
| Java 디렉토리 | `java/com/saferoute/` | `java/com/safetyroad/` |
| DB명 | `saferoute` | `safetyroad_db` |
| 브라우저 탭 타이틀 | SafeRoute | SafetyRoad |

### 2. 핵심 기능 구현: 안전 경로 스코어링
`reports.md`에서 미구현으로 표시되었던 **경로별 안전도 스코어링**과 **안전 경로 추천**이 완료되었습니다.

---

## 구현된 로직 상세

### 안전 점수 계산 방식 (`RouteService.java`)

**최종 점수 = 위험구역 점수(60%) + 시설밀도 점수(40%)**

#### 위험구역 점수 (calcDangerZoneScore)
- 경로 좌표를 **10개마다 1개씩 샘플링**
- 각 좌표가 속한 `danger_zone`의 `safetyScore` 평균 계산
- 위험구역이 없는 구간은 기본값 **50점** 처리
- 점수 범위: 0~100 (높을수록 안전)

#### 시설밀도 점수 (calcFacilityDensityScore)
- 경로 좌표를 **10개마다 1개씩 샘플링**
- 각 좌표 반경 **100m(≈0.001도)** 내 CCTV/보안등 수 집계
- 시설 수 → 점수 환산 기준:

| 반경 내 시설 수 | 점수 |
|----------------|------|
| 0개 | 0점 |
| 1개 | 30점 |
| 2개 | 40점 |
| 3~5개 | 47~61점 |
| 6~10개 | 65~81점 |
| 11개 이상 | 최대 100점 |

#### 추천 경로 선정 (isRecommended)
- 3개 경로(추천/대로우선/최단) 중 **safetyScore가 가장 높은 경로에 `isRecommended: true`** 설정
- 나머지 경로는 `isRecommended: false`

---

## 현재 API 응답 스펙

### GET /api/routes/safe
```
GET /api/routes/safe?startLat={}&startLng={}&endLat={}&endLng={}
```

**응답 예시:**
```json
[
  {
    "routeId": "uuid",
    "coordinates": [[37.38, 127.12], [37.39, 127.13]],
    "totalDistance": 1200,
    "totalTime": 900,
    "safetyScore": 62.4,
    "recommended": true,
    "searchOption": "최단"
  },
  {
    "routeId": "uuid",
    "coordinates": [[37.38, 127.12], [37.40, 127.13]],
    "totalDistance": 1500,
    "totalTime": 1100,
    "safetyScore": 55.8,
    "recommended": false,
    "searchOption": "추천"
  },
  {
    "routeId": "uuid",
    "coordinates": [[37.38, 127.12], [37.41, 127.13]],
    "totalDistance": 1600,
    "totalTime": 1200,
    "safetyScore": 55.8,
    "recommended": false,
    "searchOption": "대로우선"
  }
]
```

**필드 설명:**
| 필드 | 타입 | 설명 |
|------|------|------|
| routeId | String | UUID, 경로 식별자 |
| coordinates | `[[lat, lng], ...]` | 경로 좌표 배열 (위도, 경도 순) |
| totalDistance | int | 총 거리 (미터) |
| totalTime | int | 총 시간 (초) |
| safetyScore | double | 안전 점수 0~100, 높을수록 안전 |
| recommended | boolean | 가장 안전한 경로 여부 |
| searchOption | String | "추천" / "대로우선" / "최단" |

### 나머지 API (변경 없음)
- `GET /api/search/pois?keyword={}` — 장소 검색
- `GET /api/danger-zones` — 위험구역 목록
- `POST /api/danger-zones/recalculate` — 위험구역 재계산
- `POST /api/opendata/sync` — 공공데이터 동기화

---

## DB 현황

- DB명: `safetyroad_db` (PostgreSQL + PostGIS)
- `safety_facility`: CCTV 1,735건 + 보안등 3,064건 = **총 4,799건**
- `danger_zone`: 분당구 300m 격자 **1,085개**

### 새 환경에서 DB 세팅 순서
```sql
-- 1. DB 생성
CREATE DATABASE safetyroad_db;

-- 2. safetyroad_db 접속 후 PostGIS 설치
CREATE EXTENSION postgis;
```
```
-- 3. 백엔드 실행 후 API 호출
POST /api/opendata/sync              # 공공데이터 수집 (약 1~2분)
POST /api/danger-zones/recalculate   # 위험구역 격자 재계산
```

---

## 보안 설정

- `backend/src/main/resources/application.yml` — `.gitignore`에 추가됨 (API 키 포함)
- `frontend/.env` — `.gitignore`에 추가됨
- `frontend/public/index.html` 카카오 JS 키 → `%REACT_APP_KAKAO_JS_KEY%` 환경변수로 교체
- `frontend/.env.example` 파일 생성됨 (팀원 가이드용)

### 프론트엔드 .env 설정 방법
`frontend/.env` 파일을 직접 생성해서 아래 값 입력:
```
REACT_APP_TMAP_APP_KEY=실제키값
REACT_APP_KAKAO_JS_KEY=실제키값
```

---

## 프론트엔드 개발 가이드

### 지금 바로 작업 가능한 항목

#### 1. 안전 경로 UI (최우선)
`src/components/Route/RouteResult.js` 수정:
- **안전 점수 시각화** — `safetyScore` 값을 색상/프로그레스 바로 표시
  - 0~40점: 빨강 (위험)
  - 41~70점: 주황 (보통)
  - 71~100점: 초록 (안전)
- **추천 뱃지** — `recommended: true`인 경로에 "안전 추천" 뱃지 표시

#### 2. 경로 색상 차별화
`src/components/Map/RouteOverlay.js` 수정:
- 현재 모든 경로가 같은 색으로 표시됨
- `safetyScore` 또는 `recommended` 값에 따라 색상 분기 필요
- 추천 경로: 초록, 일반 경로: 파랑/회색

#### 3. 위험구역 상세 팝업
`src/components/Map/DangerZoneOverlay.js` 수정:
- 위험구역 클릭 시 팝업으로 `riskLevel`, `safetyScore`, `facilityCount` 표시

#### 4. 원클릭 안전 경로 안내
- "가장 안전한 경로로 안내" 버튼 추가
- `recommended: true`인 경로 자동 선택

### 현재 RouteResult.js 상태
```jsx
// 29번째 줄 근처 - safetyScore 이미 표시 중, 스타일 추가만 하면 됨
<span className="route-safety">
  안전점수: {route.safetyScore != null ? route.safetyScore : '-'}
</span>
```
`route.safetyScore`와 `route.recommended` 두 필드 모두 실제 값이 채워져 있으므로 바로 활용 가능합니다.

---

## 향후 개선 예정 (지금 하지 않아도 됨)
- 좌표 정확도 검증 및 가중치 조정
- 가로등, 편의점, 경찰서 등 추가 시설 데이터 수집
- 공공데이터 주기적 자동 동기화 (`@Scheduled`)
