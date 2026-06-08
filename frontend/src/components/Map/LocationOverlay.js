import { useEffect, useRef } from 'react';

export default function LocationOverlay({ map, location }) {
  const overlayRef = useRef(null);

  useEffect(() => {
    const kakao = window.kakao;
    if (!map || !kakao || !location) return;

    if (overlayRef.current) overlayRef.current.setMap(null);

    const content = `
      <div style="position:relative;width:16px;height:16px;">
        <div style="
          position:absolute;width:36px;height:36px;
          background:rgba(66,133,244,0.2);border-radius:50%;
          top:-10px;left:-10px;
        "></div>
        <div style="
          width:16px;height:16px;
          background:#4285F4;border:3px solid #fff;
          border-radius:50%;
          box-shadow:0 2px 6px rgba(66,133,244,0.6);
          position:relative;z-index:1;
        "></div>
      </div>
    `;

    overlayRef.current = new kakao.maps.CustomOverlay({
      content,
      position: new kakao.maps.LatLng(location.lat, location.lng),
      zIndex: 30,
    });
    overlayRef.current.setMap(map);

    return () => {
      if (overlayRef.current) overlayRef.current.setMap(null);
    };
  }, [map, location]);

  return null;
}
