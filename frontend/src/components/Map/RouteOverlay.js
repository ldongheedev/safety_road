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

export default function RouteOverlay({ map, routes, selectedRouteId }) {
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
    });

    fitBounds(map, routes);
  }, [map, routes, selectedRouteId]);

  return null;
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
