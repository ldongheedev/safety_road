# 코드 컨벤션 (Code Conventions)

실제 코드베이스에서 관찰된 규칙이다. 새 코드는 이 패턴을 따른다.

## 공통
- 들여쓰기: 4칸 공백(Java), 2칸 공백(JS).
- 주석과 사용자 노출 문자열은 한국어를 사용한다 (예: 에러 메시지 "경로를 찾을 수 없습니다").
- 좌표는 항상 `(lat, lng)` 순서로 다룬다. 단, PostGIS 함수에 넘길 때는 `ST_Point(lng, lat)`처럼 경도-위도 순으로 전달한다 (좌표 순서 실수 주의).

## 백엔드 (Java / Spring Boot)

### 패키지 구조
`comp.soft.backend` 하위에 계층별 패키지로 분리:
`config`, `controller`, `dto`, `entity`, `external`, `repository`, `service`

### 의존성 주입
- 생성자 주입만 사용한다. `@Autowired` 필드 주입을 쓰지 않는다.
- 필드는 `private final`로 선언하고 생성자에서 주입받는다.

```java
private final RouteService routeService;

public RouteController(RouteService routeService) {
    this.routeService = routeService;
}
```

### 컨트롤러
- `@RestController` + 클래스 레벨 `@RequestMapping("/api/...")`.
- 입력값 검증은 컨트롤러에서 수행하고, 실패 시 `ResponseEntity.badRequest().build()` 반환.
- 정상 응답은 `ResponseEntity.ok(...)`.
- 검증 헬퍼는 `private boolean` 메서드로 분리 (예: `isValidKoreaLat`).

### 엔티티
- POJO + Lombok (`@Getter @Setter @NoArgsConstructor`). JPA 어노테이션 없음.
- 좌표는 엔티티에서 `double latitude`, `double longitude` 필드로 보관 (DB의 PostGIS `location` 컬럼과는 `ST_X`/`ST_Y`로 변환).

### 리포지토리 (JdbcTemplate)
- `@Repository` + 생성자 주입된 `JdbcTemplate`.
- `RowMapper`는 람다로 정의해 필드로 보관한다.
- 반복되는 SELECT 절은 `private static final String` 상수로 추출한다 (예: `SELECT_COORDS`).
- 쓰기 메서드(DELETE 등)에는 `@Transactional`을 붙인다.
- 파라미터는 `?` 바인딩만 사용한다. 문자열 연결로 값을 SQL에 삽입하지 않는다.
- `queryForObject`로 카운트 조회 시 `null` 가드 후 기본값 반환:

```java
Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
return count != null ? count : 0;
```

- 대량 삽입은 `jdbcTemplate.batchUpdate`를 사용한다.
- **공간 컬럼 ↔ 엔티티 변환**: DB는 PostGIS 지오메트리로 저장하고, SELECT에서 평면 값으로 꺼내 엔티티에 매핑한다.
  - `safety_facility.location`(Point): `ST_Y(location) AS latitude`, `ST_X(location) AS longitude`로 조회. 저장 시 `ST_SetSRID(ST_Point(lng, lat), 4326)`.
  - `danger_zone.grid_polygon`(Polygon): `ST_YMin/ST_XMin/ST_YMax/ST_XMax`로 bounds(min/max lat·lng)를 꺼내 엔티티 필드로 매핑. 저장 시 `ST_MakeEnvelope(minLng, minLat, maxLng, maxLat, 4326)`. **DB에 min/max 컬럼은 없다** — 엔티티의 minLat 등은 폴리곤에서 파생된 가상 값이다.
  - 점 포함 판정은 `ST_Intersects(grid_polygon, ST_SetSRID(ST_Point(lng, lat), 4326))`.

### 서비스
- `@Service` + 생성자 주입.
- 점수/계산 로직은 작은 `private` 메서드로 분리한다 (예: `calcDangerZoneScore`, `calcFacilityDensityScore`, `facilityCountToScore`).
- 매직넘버는 메서드 내부 `final` 지역 상수로 선언한다 (예: `final double RADIUS_DEGREES = 0.001;`).
- 반올림은 `Math.round(value * 10.0) / 10.0` 패턴(소수 첫째자리).

## 프론트엔드 (React)

### 컴포넌트
- 함수형 컴포넌트 + 훅만 사용한다. 클래스 컴포넌트 금지.
- `export default function ComponentName({ props }) { ... }` 형태로 정의한다.
- 컴포넌트 파일은 PascalCase (`SearchBar.js`), 훅 파일은 `useXxx.js` 카멜케이스.

### 상태/로직
- 재사용 비동기 로직은 커스텀 훅으로 분리한다 (예: `useRoute`).
- 이벤트 핸들러/검색 함수는 `useCallback`으로 감싼다.
- 비동기 호출은 `loading` / `error` / 데이터 상태 3종을 함께 관리하고, `try/catch/finally`로 처리한다.

### API 호출
- `src/api/index.js`의 공용 axios 인스턴스(`api`)를 import 해서 사용한다.
- baseURL이 `/api`이므로 호출 시 그 하위 경로만 적는다 (예: `api.get('/routes/safe', { params })`).
- 절대 URL이나 별도 axios 인스턴스를 만들지 않는다.

### 스타일
- className 기반 글로벌 CSS. 케밥케이스 클래스명 (`search-bar`, `route-safety-bar-fill`).
- 인라인 스타일은 동적 값(점수에 따른 색상 등)에만 제한적으로 사용한다.

### 안전 점수 표시 기준
`safetyUtils.js`의 임계값을 단일 출처로 사용한다 (중복 정의 금지):
- 71점 이상: 안전 (`#22c55e`)
- 41~70점: 보통 (`#f97316`)
- 40점 이하: 위험 (`#ef4444`)
- null: 알 수 없음 (`#6b7280`)

## Git / 커밋 관례
- `main` 단일 브랜치로 운용한다 (feature 브랜치 없음).
- 커밋 메시지는 **한글**로 작성하고 prefix를 붙인다: `feat:`, `fix:`, `refactor:`, `chore:`.
  - 예: `feat: 성남시 CCTV/보안등 데이터 추가 및 시설 마커 개선`
- 푸시 전 API 키 등 민감정보가 `.gitignore`에 포함됐는지 확인한다 (CLAUDE.md 규칙).

## 테스트 / 린터 현황
- 테스트 작성 관례가 없다. 백엔드는 `BackendApplicationTests`만 있고 `@SpringBootTest`가 주석 처리된 빈 테스트다. 프론트엔드에 `*.test.js` 없음.
- 별도 포매터/린터 설정 파일이 없다 (ESLint·Prettier·Checkstyle·Spotless 설정 없음). 프론트는 CRA 내장 ESLint만 동작한다.
- 사용자가 명시적으로 요청하지 않는 한 테스트나 린터 설정을 새로 추가하지 않는다.

## 알려진 코드 스타일 특이점
- `SearchController`는 외부 API 응답을 raw `Map`으로 파싱한다(타입 경고 다수 존재). 기존 코드를 수정할 때 굳이 제네릭화하지 않아도 되나, 새 코드는 가능하면 타입을 명시한다.
