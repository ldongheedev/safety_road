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

const RADIUS_KM = 2;

function getRouteBoundingBox(routes, selectedRouteId) {
  const targetRoute = routes.find((r) => r.routeId === selectedRouteId)
    || routes.find((r) => r.recommended)
    || routes[0];

  if (!targetRoute?.coordinates?.length) return null;

  const lats = targetRoute.coordinates.map(([lat]) => lat);
  const lngs = targetRoute.coordinates.map(([, lng]) => lng);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  return {
    minLat: minLat - 0.02,
    maxLat: maxLat + 0.02,
    minLng: minLng - 0.02,
    maxLng: maxLng + 0.02,
  };
}

function getZoneCenter(bounds) {
  const lats = bounds.map(([lat]) => lat);
  const lngs = bounds.map(([, lng]) => lng);
  return [
    (Math.min(...lats) + Math.max(...lats)) / 2,
    (Math.min(...lngs) + Math.max(...lngs)) / 2,
  ];
}

function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export default function DangerZoneOverlay({ map, zones, visible, origin, routes, selectedRouteId }) {
  const polygonsRef = useRef([]);
  const overlaysRef = useRef([]);
  const popupOverlayRef = useRef(null);

  // 팝업 닫기 전역 함수 등록
  useEffect(() => {
    window.__closeDzPopup = () => {
      if (popupOverlayRef.current) {
        popupOverlayRef.current.setMap(null);
        popupOverlayRef.current = null;
      }
    };
    return () => {
      delete window.__closeDzPopup;
    };
  }, []);

  // 언마운트 시 팝업 cleanup
  useEffect(() => {
    return () => {
      popupOverlayRef.current?.setMap(null);
    };
  }, []);

  useEffect(() => {
    // 기존 폴리곤/오버레이 제거
    polygonsRef.current.forEach((p) => p.setMap(null));
    polygonsRef.current = [];
    overlaysRef.current.forEach((o) => o.setMap(null));
    overlaysRef.current = [];

    const kakao = window.kakao;

    if (!map || !zones.length || !visible || !kakao) return;

    // 경로 결과가 있으면 선택 경로 bounding box 기준, 없으면 origin 반경 2km 기준
    let visibleZones;
    if (routes && routes.length > 0) {
      const bbox = getRouteBoundingBox(routes, selectedRouteId);
      if (!bbox) return;
      const filtered = zones.filter((zone) => {
        const pts = zone.bounds;
        if (!pts || pts.length === 0) return false;
        const avgLat = pts.reduce((s, p) => s + p[0], 0) / pts.length;
        const avgLng = pts.reduce((s, p) => s + p[1], 0) / pts.length;
        return avgLat >= bbox.minLat && avgLat <= bbox.maxLat &&
               avgLng >= bbox.minLng && avgLng <= bbox.maxLng;
      });
      visibleZones = filtered;
    } else {
      if (!origin) return;
      visibleZones = zones.filter((zone) => {
        if (!zone.bounds || zone.bounds.length === 0) return false;
        const [cLat, cLng] = getZoneCenter(zone.bounds);
        return haversineKm(origin.lat, origin.lng, cLat, cLng) <= RADIUS_KM;
      });
    }

    visibleZones.forEach((zone) => {
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
        // 기존 팝업 CustomOverlay 제거
        if (popupOverlayRef.current) {
          popupOverlayRef.current.setMap(null);
          popupOverlayRef.current = null;
        }

        const content = `
          <div class="dz-popup">
            <div class="dz-popup-header dz-popup-header--${zone.riskLevel.toLowerCase()}">
              <span class="dz-popup-level">${label}</span>
              <button class="dz-popup-close" onclick="window.__closeDzPopup()">✕</button>
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
        popupOverlayRef.current = overlay;
        overlaysRef.current.push(overlay);
      });
    });

    // 지도 클릭 시 팝업 닫기
    const mapClickListener = () => {
      if (popupOverlayRef.current) {
        popupOverlayRef.current.setMap(null);
        popupOverlayRef.current = null;
      }
    };
    kakao.maps.event.addListener(map, 'click', mapClickListener);

    return () => {
      kakao.maps.event.removeListener(map, 'click', mapClickListener);
    };
  }, [map, zones, visible, origin, routes, selectedRouteId]);

  return null;
}
