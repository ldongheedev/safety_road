# 기술 스택 (Tech Stack)

SafetyRoad는 성남시 대상 AI 기반 안전 도보 경로 추천 웹 애플리케이션이다. 백엔드(Spring Boot)와 프론트엔드(React)로 구성된 모노레포 구조다.

## 백엔드

| 항목 | 내용 |
|------|------|
| 언어 | Java 25 (Eclipse Adoptium JDK 25) |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 도구 | Maven (`mvnw` / `mvnw.cmd` 래퍼 사용) |
| DB | PostgreSQL + PostGIS (DB명 `safetyroad_db`, 포트 5432) |
| 데이터 접근 | **순수 JdbcTemplate** (JPA/Hibernate 사용 안 함) |
| HTTP 클라이언트 | `java.net.http.HttpClient` (JDK 내장) 직접 사용 |
| 보일러플레이트 | Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) |
| JSON | Jackson (`jackson-databind`) |
| 공간 연산 | PostGIS 함수 (ST_Within, ST_DWithin, ST_Distance, ST_MakeEnvelope, ST_Point, ST_SetSRID, ST_X, ST_Y) |

### 주의사항
- `pom.xml`에는 JPA 의존성이 없다. ORM이 필요해 보여도 JPA를 추가하지 말고 JdbcTemplate 패턴을 따른다. (과거 JPA/Hibernate Spatial을 쓰다 JdbcTemplate으로 전환한 이력 있음)
- 외부 API 호출은 두 방식이 혼재한다. **공공데이터/Tmap 경로 동기화 계열은 `HttpClient`(JDK 내장)** 를 직접 사용하고, **`SearchController`(Tmap POI 검색)만 WebClient**를 사용한다. 새 외부 호출은 해당 도메인의 기존 패턴을 따른다.
- DB 마이그레이션 도구(Flyway/Liquibase)와 auto-ddl이 없다. 스키마는 수동 관리된다. (상세는 `structure.md` 참고)

## 프론트엔드

| 항목 | 내용 |
|------|------|
| 언어 | JavaScript (TypeScript 아님) |
| 프레임워크 | React 18 (Create React App, `react-scripts` 5) |
| 지도 | Kakao Maps JavaScript API |
| HTTP | Axios (`src/api/index.js`의 공용 인스턴스) |
| 스타일 | 글로벌 CSS (`src/styles/global.css`) — CSS 프레임워크 없음 |
| 개발 포트 | 3000 (백엔드 8080으로 프록시) |

### 주의사항
- `package.json`의 `"proxy": "http://localhost:8080"` 설정으로 `/api` 요청이 백엔드로 전달된다. API 호출은 항상 상대경로 `/api/...`를 사용한다.
- TypeScript, CSS-in-JS, 상태관리 라이브러리(Redux 등)를 새로 도입하지 않는다.

### 환경변수
프론트엔드 키는 `frontend/.env`에 `REACT_APP_` 접두사로 주입한다.
- `REACT_APP_TMAP_APP_KEY`
- `REACT_APP_KAKAO_JS_KEY` — Kakao 지도 JS 키. `KakaoMap.js`에서 `process.env.REACT_APP_KAKAO_JS_KEY`로 사용.

백엔드 키는 `backend/.env`에 두며 `application.yml`의 `${...}` 기본값을 오버라이드한다.
- ⚠️ `DATA_ILLUMINATION_KEY`는 현재 `backend/.env`에 빠져 있어 `application.yml`의 하드코딩 기본값으로만 동작한다.

## 실행 명령어

### 백엔드
```cmd
cd backend
mvnw spring-boot:run
```
또는 루트의 `start-backend.bat` 실행 (JAVA_HOME 설정 포함).

### 프론트엔드
```cmd
cd frontend
npm start
```
또는 루트의 `start-frontend.bat` 실행.

### 빌드
```cmd
cd backend
mvnw clean package

cd frontend
npm run build
```

> 개발 서버(`npm start`, `mvnw spring-boot:run`)는 종료되지 않는 장기 실행 프로세스다. 자동화 도구로 실행하지 말고 사용자가 직접 터미널에서 실행하도록 안내한다.

## 보안 규칙
- API 키가 담긴 `backend/src/main/resources/application.yml`은 `.gitignore` 처리되어 있다.
- 푸시 전 항상 API 키 등 민감정보가 `.gitignore`에 포함됐는지 확인한다 (CLAUDE.md 규칙).
