import { useEffect, useRef } from 'react';

export default function TmapView({ onMapReady }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);

  useEffect(() => {
    const Tmapv2 = window.Tmapv2;
    if (!Tmapv2 || mapRef.current) return;

    const map = new Tmapv2.Map(containerRef.current, {
      center: new Tmapv2.LatLng(37.5665, 126.978),
      width: '100%',
      height: '100%',
      zoom: 15,
    });

    mapRef.current = map;
    if (onMapReady) onMapReady(map);
  }, []);

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', position: 'absolute', top: 0, left: 0 }}
    />
  );
}
