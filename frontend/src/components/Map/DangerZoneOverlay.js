import { useEffect, useRef } from 'react';

const RISK_COLORS = {
  HIGH: { fill: '#d32f2f', stroke: '#d32f2f' },
  MEDIUM: { fill: '#ff9800', stroke: '#ff9800' },
  LOW: { fill: '#4caf50', stroke: '#4caf50' },
};

const RISK_LABEL = {
  HIGH: '위험',
  MEDIUM: '주의',
  LOW: '안전',
};

export default function DangerZoneOverlay({ map, zones, visible }) {
  const polygonsRef = useRef([]);
  const overlaysRef = useRef([]);

  useEffect(() => {
    // 기존 오버레이 제거
    polygonsRef.current.forEach((p) => p.setMap(null));
    polygonsRef.current = [];
    overlaysRef.current.forEach((o) => o.setMap(null));
    overlaysRef.current = [];

    const kakao = window.kakao;
    if (!map || !zones.length || !visible || !kakao) return;

    zones.forEach((zone) => {
      const colors = RISK_COLORS[zone.riskLevel] || RISK_COLORS.MEDIUM;
      const label = RISK_LABEL[zone.riskLevel] || '보통';

      const path = zone.bounds.map(
        ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
      );

      const polygon = new kakao.maps.Polygon({
        path,
        strokeWeight: 1,
        strokeColor: colors.stroke,
        strokeOpacity: 0.8,
        fillColor: colors.fill,
        fillOpacity: zone.riskLevel === 'HIGH' ? 0.35 : zone.riskLevel === 'MEDIUM' ? 0.25 : 0.15,
      });

      polygon.setMap(map);
      polygonsRef.current.push(polygon);

      // 클릭 시 팝업 (CustomOverlay)
      kakao.maps.event.addListener(polygon, 'click', (mouseEvent) => {
        // 기존 팝업 닫기
        overlaysRef.current.forEach((o) => o.setMap(null));
        overlaysRef.current = [];

        const content = `
          <div class="dz-popup">
            <div class="dz-popup-header dz-popup-header--${zone.riskLevel.toLowerCase()}">
              <span class="dz-popup-level">${label}</span>
              <button class="dz-popup-close" onclick="this.closest('.dz-popup-wrap').remove()">✕</button>
            </div>
            <div class="dz-popup-body">
              <div class="dz-popup-row">
                <span class="dz-popup-label">안전 점수</span>
                <span class="dz-popup-value">${zone.safetyScore != null ? zone.safetyScore.toFixed(1) : '-'}점</span>
              </div>
              <div class="dz-popup-row">
                <span class="dz-popup-label">시설 수</span>
                <span class="dz-popup-value">${zone.facilityCount}개</span>
              </div>
              <div class="dz-popup-bar">
                <div class="dz-popup-bar-fill dz-popup-bar-fill--${zone.riskLevel.toLowerCase()}" style="width:${zone.safetyScore ?? 0}%"></div>
              </div>
            </div>
          </div>
        `;

        const overlay = new kakao.maps.CustomOverlay({
          content: `<div class="dz-popup-wrap">${content}</div>`,
          position: mouseEvent.latLng,
          yAnchor: 1.1,
          zIndex: 30,
        });

        overlay.setMap(map);
        overlaysRef.current.push(overlay);
      });
    });

    // 지도 클릭 시 팝업 닫기
    const mapClickListener = () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];
    };
    kakao.maps.event.addListener(map, 'click', mapClickListener);

    return () => {
      kakao.maps.event.removeListener(map, 'click', mapClickListener);
    };
  }, [map, zones, visible]);

  return null;
}
