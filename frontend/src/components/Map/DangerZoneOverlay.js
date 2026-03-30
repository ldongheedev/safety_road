import { useEffect, useRef } from 'react';

const RISK_COLORS = {
  HIGH: { fill: 'rgba(211, 47, 47, 0.35)', stroke: '#d32f2f' },
  MEDIUM: { fill: 'rgba(255, 152, 0, 0.3)', stroke: '#ff9800' },
  LOW: { fill: 'rgba(76, 175, 80, 0.2)', stroke: '#4caf50' },
};

export default function DangerZoneOverlay({ map, zones, visible }) {
  const rectanglesRef = useRef([]);

  useEffect(() => {
    // 기존 오버레이 제거
    rectanglesRef.current.forEach((rect) => rect.setMap(null));
    rectanglesRef.current = [];

    const kakao = window.kakao;
    if (!map || !zones.length || !visible || !kakao) return;

    zones.forEach((zone) => {
      const colors = RISK_COLORS[zone.riskLevel] || RISK_COLORS.MEDIUM;

      // bounds: [[lat,lng], [lat,lng], ...] (polygon 꼭짓점)
      const path = zone.bounds.map(
        ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
      );

      const polygon = new kakao.maps.Polygon({
        path,
        strokeWeight: 1,
        strokeColor: colors.stroke,
        strokeOpacity: 0.8,
        fillColor: colors.fill.replace(/rgba?\([^)]+\)/, colors.stroke),
        fillOpacity: zone.riskLevel === 'HIGH' ? 0.35 : zone.riskLevel === 'MEDIUM' ? 0.25 : 0.15,
      });

      polygon.setMap(map);
      rectanglesRef.current.push(polygon);
    });
  }, [map, zones, visible]);

  return null;
}
