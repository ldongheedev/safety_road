# SafeRoute (SafetyRoad)

성남시(분당구·수정구·중원구) 야간 귀가길 안전 시스템. 보안등, CCTV, 경찰시설 등 공공시설물 데이터를 기반으로 더 안전한 경로를 추천합니다.

## 핵심 기능

- **안전 경로 추천**: 출발지-목적지 경로 중 보안등·CCTV·경찰시설이 많은 경로에 안전 점수를 매겨 우선 추천
- **위험구역 표시**: 지역을 그리드로 나눠 안전시설 밀도 기반 위험도(risk level)를 계산하고 지도에 표시
- **실시간 위치 추적 및 위험구역 알림**: 현재 위치가 위험구역에 진입하면 알림
- **장소 검색**: Tmap POI 검색으로 목적지 탐색
- **SOS 버튼**: 비상 상황 대응 기능
- **모바일/웹 반응형 UI**

## 기술 스택

- **백엔드**: Spring Boot 4, Java 25, Spring JDBC(JdbcTemplate), PostgreSQL + PostGIS
- **프론트엔드**: React 18(Create React App), Kakao Maps JS SDK
- **외부 API**: Tmap(경로 탐색, POI 검색), 카카오맵, 공공데이터포털/경기데이터드림(CCTV, 보안등, 조도등, 경찰시설)

## 프로젝트 구조

```
backend/   Spring Boot API 서버 (포트 8080)
frontend/  React 앱 (Create React App, 포트 3000)
```

## 실행 방법

### 백엔드

1. PostgreSQL(+ PostGIS 확장)을 준비하고 `backend/src/main/resources/application.yml`에 DB 접속 정보와 API 키(Tmap, 카카오, 공공데이터포털 등)를 설정합니다. (이 파일은 `.gitignore`에 포함되어 있으므로 직접 생성해야 합니다.)
2. 아래 명령으로 실행합니다.

```bash
cd backend
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

### 프론트엔드

1. `frontend/.env`에 아래 값을 설정합니다.

```
REACT_APP_TMAP_APP_KEY=...
REACT_APP_KAKAO_JS_KEY=...
```

2. 아래 명령으로 실행합니다.

```bash
cd frontend
npm install
npm start
```

개발 서버는 `/api` 요청을 `http://localhost:8080`(백엔드)으로 프록시합니다.

### 외부 공개(ngrok)

고정 도메인으로 외부에 노출하려면 루트의 `start-ngrok.bat`을 실행합니다(프론트엔드가 3000번 포트에서 실행 중이어야 함).

## 주요 API

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/health` | 헬스 체크 |
| GET | `/api/facilities` | 안전시설물 조회 |
| GET | `/api/danger-zones` | 위험구역 조회 |
| POST | `/api/danger-zones/recalculate` | 위험구역 재계산 |
| GET | `/api/routes/safe` | 안전 경로 추천 |
| GET | `/api/search/pois` | 장소(POI) 검색 |
| POST | `/api/opendata/sync` | 공공데이터 전체 동기화(운영용) |

## 팀

김이상궁 — 김령균(프론트엔드/백엔드), 이시우(백엔드), 이동희(백엔드, 저장소 관리)
