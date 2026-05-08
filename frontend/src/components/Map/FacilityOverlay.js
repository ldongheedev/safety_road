import { useEffect, useRef } from 'react';

const FACILITY_CONFIG = {
  CCTV: { color: '#3b82f6', label: '📹 CCTV' },
  SECURITY_LIGHT: { color: '#eab308', label: '💡 보안등' },
  POLICE_BOX: { color: '#1d4ed8', label: '🚔 치안센터' },
  DISTRICT_POLICE: { color: '#7c3aed', label: '🚓 지구대' },
};

// Kakao Maps: level 1(최대 줌인) ~ 14(최대 줌아웃)
// 시설 마커는 충분히 줌인된 상태(level ≤ 6)에서만 표시
const MAX_KAKAO_LEVEL = 6;

export default function FacilityOverlay({ map, visible, bounds, zoom }) {
  const markersRef = useRef([]);
  const tooltipRef = useRef(null);

  // 툴팁 전역 함수 등록
  useEffect(() => {
    window.__showFacilityTooltip = (label, lat, lng) => {
      const kakao = window.kakao;
      if (!map || !kakao) return;
      if (tooltipRef.current) {
        tooltipRef.current.setMap(null);
        tooltipRef.current = null;
      }
      const content = `
        <div style="
          background:rgba(0,0,0,0.72);
          color:#fff;
          padding:4px 8px;
          border-radius:4px;
          font-size:12px;
          white-space:nowrap;
          pointer-events:none;
        ">${label}</div>
      `;
      const tooltip = new kakao.maps.CustomOverlay({
        content,
        position: new kakao.maps.LatLng(lat, lng),
        yAnchor: 2.6,
        zIndex: 20,
      });
      tooltip.setMap(map);
      tooltipRef.current = tooltip;
    };

    window.__closeFacilityTooltip = () => {
      if (tooltipRef.current) {
        tooltipRef.current.setMap(null);
        tooltipRef.current = null;
      }
    };

    return () => {
      delete window.__showFacilityTooltip;
      delete window.__closeFacilityTooltip;
    };
  }, [map]);

  // 언마운트 cleanup
  useEffect(() => {
    return () => {
      markersRef.current.forEach((m) => m.setMap(null));
      tooltipRef.current?.setMap(null);
    };
  }, []);

  useEffect(() => {
    // 기존 마커·툴팁 전부 제거
    markersRef.current.forEach((m) => m.setMap(null));
    markersRef.current = [];
    if (tooltipRef.current) {
      tooltipRef.current.setMap(null);
      tooltipRef.current = null;
    }

    const kakao = window.kakao;
    if (!map || !kakao || !visible || !bounds || zoom > MAX_KAKAO_LEVEL) return;

    const { lat1, lng1, lat2, lng2 } = bounds;
    const base = `/api/facilities?lat1=${lat1}&lng1=${lng1}&lat2=${lat2}&lng2=${lng2}&limit=1000`;
    const controller = new AbortController();
    const signal = controller.signal;

    const renderFacilities = (facilities) => {
      facilities.forEach((f) => {
        const config = FACILITY_CONFIG[f.facilityType];
        if (!config) return;

        // 경찰 시설은 크기를 키워서 시각적으로 구분
        const isPolice = f.facilityType === 'POLICE_BOX' || f.facilityType === 'DISTRICT_POLICE';
        const size = isPolice ? '12px' : '8px';

        const content = `
          <div
            onclick="window.__showFacilityTooltip('${config.label}', ${f.lat}, ${f.lng})"
            style="
              width:${size};
              height:${size};
              background:${config.color};
              border:1px solid #fff;
              border-radius:50%;
              box-shadow:0 1px 3px rgba(0,0,0,0.4);
              cursor:pointer;
            "
          ></div>
        `;

        const marker = new kakao.maps.CustomOverlay({
          content,
          position: new kakao.maps.LatLng(f.lat, f.lng),
          zIndex: isPolice ? 15 : 10,
        });
        marker.setMap(map);
        markersRef.current.push(marker);
      });
    };

    Promise.all([
      fetch(`${base}&type=CCTV`, { signal }).then((r) => r.json()),
      fetch(`${base}&type=SECURITY_LIGHT`, { signal }).then((r) => r.json()),
      fetch(`${base}&type=POLICE_BOX`, { signal }).then((r) => r.json()),
      fetch(`${base}&type=DISTRICT_POLICE`, { signal }).then((r) => r.json()),
    ])
      .then(([cctv, lights, policeBox, districtPolice]) => {
        renderFacilities(cctv);
        renderFacilities(lights);
        renderFacilities(policeBox);
        renderFacilities(districtPolice);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') console.error('시설 데이터 로드 실패:', err);
      });

    return () => controller.abort();
  }, [map, visible, bounds, zoom]);

  return null;
}
