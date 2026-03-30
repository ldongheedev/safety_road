import { useEffect, useRef } from 'react';

export default function RouteOverlay({ map, routes, selectedRouteId }) {
  const polylinesRef = useRef([]);

  useEffect(() => {
    // 기존 폴리라인 제거
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
      const isRecommended = route.recommended;

      let strokeColor = '#999999';
      let strokeWeight = 4;

      if (isSelected) {
        strokeColor = '#FF6B00';
        strokeWeight = 6;
      } else if (isRecommended) {
        strokeColor = '#2DB400';
        strokeWeight = 6;
      }

      const path = route.coordinates.map(
        ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
      );

      const polyline = new kakao.maps.Polyline({
        path,
        strokeColor,
        strokeWeight,
        strokeOpacity: isSelected ? 1 : 0.6,
        strokeStyle: 'solid',
      });

      polyline.setMap(map);
      polylinesRef.current.push(polyline);
    });

    // 지도 범위 자동 조정
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
