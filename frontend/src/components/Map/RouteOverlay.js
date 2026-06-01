import { useEffect, useRef } from 'react';
import { getLevelColor } from '../../utils/safetyUtils';

export default function RouteOverlay({ map, routes, selectedRouteId }) {
  const polylinesRef = useRef([]);

  useEffect(() => {
    polylinesRef.current.forEach((pl) => pl.setMap(null));
    polylinesRef.current = [];

    const kakao = window.kakao;
    if (!map || !routes.length || !kakao) return;

    const sorted = [...routes].sort((a, b) => {
      if (a.routeId === selectedRouteId) return 1;
      if (b.routeId === selectedRouteId) return -1;
      return 0;
    });

    sorted.forEach((route) => {
      const isSelected = route.routeId === selectedRouteId;
      const color = getLevelColor(route.safetyScore);

      const path = route.coordinates.map(
        ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
      );

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
