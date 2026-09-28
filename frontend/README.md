# SafeRoute Frontend

SafeRoute(SafetyRoad)의 프론트엔드입니다. Create React App(react-scripts) 기반이며, 카카오맵 위에 안전 경로와 위험구역을 표시합니다.

## 실행

```bash
npm install
npm start
```

`http://localhost:3000`에서 실행되며, `/api` 요청은 `src/setupProxy.js`를 통해 `http://localhost:8080`(백엔드)으로 프록시됩니다.

## 환경 변수

`.env` 파일에 아래 값을 설정합니다.

```
REACT_APP_TMAP_APP_KEY=...
REACT_APP_KAKAO_JS_KEY=...
```

## 주요 구조

```
src/
  components/       지도, 경로 결과, 검색, SOS, 위험 알림 UI
  components/Map/   카카오맵 오버레이(시설물, 위치, 경로)
  hooks/            현재 위치, 경로 탐색 훅
  api/              백엔드 API 클라이언트
  utils/            안전 점수 등급 계산 유틸
```

## 빌드

```bash
npm run build
```
