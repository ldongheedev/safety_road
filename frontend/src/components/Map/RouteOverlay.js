import { useEffect, useRef } from 'react';

const COLORS = {
  safe: '#2DB400',    // 초록
  caution: '#F5A623', // 노랑/주황
  danger: '#D32F2F',  // 빨강
  unknown: '#999999',
};

function getLevel(score) {
  if (score == null) return 'unknown';
  if (score >= 70) return 'safe';
  if (score >= 40) return 'caution';
  return 'danger';
}

export default function RouteOverlay({ map, routes, selectedRouteId, dangerZones }) {
  const polylinesRef = useRef([]);

  useEffect(() => {
    polylinesRef.current.forEach((pl) => pl.setMap(null));
    polylinesRef.current = [];

    const kakao = window.kakao;
    if (!map || !routes.length || !kakao) return;

    // 선택된 경로를 마지막에 그려서 위에 표시
    const sorted = [...routes].sort((a, b) => {
      if (a.routeId === selectedRouteId) return 1;
      if (b.routeId === selectedRouteId) return -1;
      return 0;
    });

    sorted.forEach((route) => {
      const isSelected = route.routeId === selectedRouteId;
      const level = getLevel(route.safetyScore);
      const color = COLORS[level];

      const path = route.coordinates.map(
        ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
      );

      // 선택된 경로: 뒤에 두꺼운 외곽선 효과
      if (isSelected) {
        const outline = new kakao.maps.Polyline({
          path,
          strokeColor: '#000',
          strokeWeight: 10,
          strokeOpacity: 0.25,
          strokeStyle: 'solid',
        });
        outline.setMap(map);
        polylinesRef.current.push(outline);
      }

      const polyline = new kakao.maps.Polyline({
        path,
        strokeColor: color,
        strokeWeight: isSelected ? 7 : 5,
        strokeOpacity: isSelected ? 1 : 0.55,
        strokeStyle: 'solid',
      });

      polyline.setMap(map);
      polylinesRef.current.push(polyline);

      // 선택된 경로의 위험구간 하이라이트
      if (isSelected && dangerZones && dangerZones.length > 0) {
        const highZones = dangerZones.filter((z) => z.riskLevel === 'HIGH' || z.riskLevel === 'MEDIUM');
        const dangerSegments = findDangerSegments(route.coordinates, highZones);

        dangerSegments.forEach((segment) => {
          const dangerPath = segment.map(
            ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
          );

          // 위험구간 경고 외곽선 (빨간 점선)
          const dangerLine = new kakao.maps.Polyline({
            path: dangerPath,
            strokeColor: '#D32F2F',
            strokeWeight: 12,
            strokeOpacity: 0.3,
            strokeStyle: 'shortdash',
          });
          dangerLine.setMap(map);
          polylinesRef.current.push(dangerLine);
        });
      }
    });

    fitBounds(map, routes);
  }, [map, routes, selectedRouteId, dangerZones]);

  return null;
}

// 경로 좌표 중 위험구역 내에 있는 연속 구간을 추출
function findDangerSegments(coordinates, zones) {
  if (!zones.length) return [];

  const segments = [];
  let current = [];

  coordinates.forEach((coord) => {
    const inDanger = zones.some((zone) => isPointInPolygon(coord, zone.bounds));

    if (inDanger) {
      current.push(coord);
    } else {
      if (current.length >= 2) {
        segments.push(current);
      }
      current = [];
    }
  });

  if (current.length >= 2) {
    segments.push(current);
  }

  return segments;
}

// 점이 폴리곤 내에 있는지 판별 (ray-casting)
function isPointInPolygon([lat, lng], bounds) {
  let inside = false;
  for (let i = 0, j = bounds.length - 1; i < bounds.length; j = i++) {
    const [yi, xi] = bounds[i];
    const [yj, xj] = bounds[j];

    if (
      yi > lat !== yj > lat &&
      lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi
    ) {
      inside = !inside;
    }
  }
  return inside;
}

function fitBounds(map, routes) {
  const kakao = window.kakao;
  const bounds = new kakao.maps.LatLngBounds();

  routes.forEach((route) => {
    route.coordinates.forEach(([lat, lng]) => {
      bounds.extend(new kakao.maps.LatLng(lat, lng));
    });
  });

  map.setBounds(bounds, 60, 60, 60, 60);
}
