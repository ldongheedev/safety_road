import { useEffect, useRef } from 'react';

export default function KakaoMap({ onMapReady, onMapClick, clickMode }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);

  useEffect(() => {
    const kakao = window.kakao;
    if (!kakao || !kakao.maps || mapRef.current) return;

    kakao.maps.load(() => {
      const map = new kakao.maps.Map(containerRef.current, {
        center: new kakao.maps.LatLng(37.3837, 127.1264), // 서현역
        level: 4,
      });

      // 지도 컨트롤 추가
      map.addControl(
        new kakao.maps.ZoomControl(),
        kakao.maps.ControlPosition.RIGHT
      );

      mapRef.current = map;
      if (onMapReady) onMapReady(map);
    });
  }, []);

  useEffect(() => {
    const kakao = window.kakao;
    const map = mapRef.current;
    if (!kakao || !map || !onMapClick || !clickMode) return;

    const listener = (mouseEvent) => {
      onMapClick(mouseEvent.latLng.getLat(), mouseEvent.latLng.getLng());
    };

    kakao.maps.event.addListener(map, 'click', listener);
    return () => kakao.maps.event.removeListener(map, 'click', listener);
  }, [onMapClick, clickMode]);

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', position: 'absolute', top: 0, left: 0, cursor: clickMode ? 'crosshair' : 'default' }}
    />
  );
}
