import { useEffect, useRef } from 'react';

export default function RouteOverlay({ map, routes, selectedRouteId }) {
  const polylinesRef = useRef([]);

  useEffect(() => {
    // 기존 폴리라인 제거
    polylinesRef.current.forEach((pl) => pl.setMap(null));
    polylinesRef.current = [];

    if (!map || !routes.length) return;

    const Tmapv2 = window.Tmapv2;

    // 선택되지 않은 경로 먼저 그리고, 선택된 경로를 위에 그림
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
        ([lat, lng]) => new Tmapv2.LatLng(lat, lng)
      );

      const polyline = new Tmapv2.Polyline({
        path,
        strokeColor,
        strokeWeight,
        strokeOpacity: isSelected ? 1 : 0.6,
        map,
      });

      polylinesRef.current.push(polyline);
    });

    // 지도 범위 자동 조정
    fitBounds(map, routes);
  }, [map, routes, selectedRouteId]);

  return null;
}

function fitBounds(map, routes) {
  const Tmapv2 = window.Tmapv2;
  let minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;

  routes.forEach((route) => {
    route.coordinates.forEach(([lat, lng]) => {
      if (lat < minLat) minLat = lat;
      if (lat > maxLat) maxLat = lat;
      if (lng < minLng) minLng = lng;
      if (lng > maxLng) maxLng = lng;
    });
  });

  const sw = new Tmapv2.LatLng(minLat, minLng);
  const ne = new Tmapv2.LatLng(maxLat, maxLng);
  const bounds = new Tmapv2.LatLngBounds(sw, ne);
  map.fitBounds(bounds, { padding: 60 });
}
