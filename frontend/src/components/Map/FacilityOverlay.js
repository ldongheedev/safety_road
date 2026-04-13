import { useEffect, useRef } from 'react';

const FACILITY_CONFIG = {
  CCTV: { color: '#3b82f6', label: '📹 CCTV' },
  SECURITY_LIGHT: { color: '#eab308', label: '💡 보안등' },
};

// Kakao Maps: level 1(최대 줌인) ~ 14(최대 줌아웃)
// 시설 마커는 충분히 줌인된 상태(level ≤ 3)에서만 표시
const MAX_KAKAO_LEVEL = 3;

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

    fetch(`/api/facilities?lat1=${lat1}&lng1=${lng1}&lat2=${lat2}&lng2=${lng2}&limit=200`)
      .then((r) => r.json())
      .then((facilities) => {
        facilities.forEach((f) => {
          const config = FACILITY_CONFIG[f.facilityType] ?? FACILITY_CONFIG.CCTV;

          const content = `
            <div
              onclick="window.__showFacilityTooltip('${config.label}', ${f.lat}, ${f.lng})"
              style="
                width:8px;
                height:8px;
                background:${config.color};
                border:1px solid #fff;
                border-radius:50%;
                box-shadow:0 1px 2px rgba(0,0,0,0.3);
                cursor:pointer;
              "
            ></div>
          `;

          const marker = new kakao.maps.CustomOverlay({
            content,
            position: new kakao.maps.LatLng(f.lat, f.lng),
            zIndex: 10,
          });
          marker.setMap(map);
          markersRef.current.push(marker);
        });
      })
      .catch((err) => console.error('시설 데이터 로드 실패:', err));
  }, [map, visible, bounds, zoom]);

  return null;
}
