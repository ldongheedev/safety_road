import { useState, useRef, useCallback, useEffect } from 'react';
import './styles/global.css';
import KakaoMap from './components/KakaoMap';
import SearchBar from './components/SearchBar';
import RouteOverlay from './components/Map/RouteOverlay';
import DangerZoneOverlay from './components/Map/DangerZoneOverlay';
import FacilityOverlay from './components/Map/FacilityOverlay';
import RouteResult from './components/Route/RouteResult';
import useRoute from './hooks/useRoute';
import useDangerZones from './hooks/useDangerZones';
import SOSButton from './components/SOSButton';

export default function App() {
  const [map, setMap] = useState(null);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [selectedRouteId, setSelectedRouteId] = useState(null);
  const [showDangerZones, setShowDangerZones] = useState(true);
  const [showFacilities, setShowFacilities] = useState(false);
  const [bounds, setBounds] = useState(null);
  const [zoom, setZoom] = useState(null);
  const originMarkerRef = useRef(null);
  const destMarkerRef = useRef(null);
  const boundsTimerRef = useRef(null);
  const { routes, loading, error, fetchRoutes } = useRoute();
  const dangerZones = useDangerZones();

  useEffect(() => {
    const recommendedRoute = (routes ?? []).find(r => r.recommended);
    if (recommendedRoute) setSelectedRouteId(recommendedRoute.routeId);
  }, [routes]);

  // 지도 뷰포트 변경 시 bounds/zoom 업데이트 (500ms 디바운스)
  useEffect(() => {
    if (!map) return;
    const kakao = window.kakao;

    const update = () => {
      clearTimeout(boundsTimerRef.current);
      boundsTimerRef.current = setTimeout(() => {
        const b = map.getBounds();
        const sw = b.getSouthWest();
        const ne = b.getNorthEast();
        setBounds({ lat1: sw.getLat(), lng1: sw.getLng(), lat2: ne.getLat(), lng2: ne.getLng() });
        setZoom(map.getLevel());
      }, 500);
    };

    update(); // 초기값 세팅
    kakao.maps.event.addListener(map, 'bounds_changed', update);
    kakao.maps.event.addListener(map, 'zoom_changed', update);

    return () => {
      clearTimeout(boundsTimerRef.current);
      kakao.maps.event.removeListener(map, 'bounds_changed', update);
      kakao.maps.event.removeListener(map, 'zoom_changed', update);
    };
  }, [map]);

  const handleMapReady = useCallback((mapInstance) => {
    setMap(mapInstance);
  }, []);

  const placeMarker = (markerRef, lat, lng, title, imageSrc) => {
    if (!map) return;
    const kakao = window.kakao;

    if (markerRef.current) {
      markerRef.current.setMap(null);
    }

    const imageSize = new kakao.maps.Size(36, 44);
    const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize);
    const position = new kakao.maps.LatLng(lat, lng);

    const marker = new kakao.maps.Marker({
      position,
      image: markerImage,
      title,
    });

    marker.setMap(map);
    markerRef.current = marker;
    map.setCenter(position);
  };

  const handleSelectOrigin = useCallback((lat, lng) => {
    setOrigin({ lat, lng });
    placeMarker(
      originMarkerRef, lat, lng, '출발지',
      'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/red_b.png'
    );
  }, [map]);

  const handleSelectDestination = useCallback((lat, lng) => {
    setDestination({ lat, lng });
    placeMarker(
      destMarkerRef, lat, lng, '목적지',
      'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/blue_b.png'
    );
  }, [map]);

  const handleSearch = () => {
    if (!origin || !destination) return;
    setSelectedRouteId(null);
    fetchRoutes(origin.lat, origin.lng, destination.lat, destination.lng);
  };

  const canSearch = origin && destination && !loading;

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <KakaoMap onMapReady={handleMapReady} />

      {/* 좌측 통합 패널 */}
      <div className="side-panel">
        <SearchBar
          onSelectOrigin={handleSelectOrigin}
          onSelectDestination={handleSelectDestination}
        />

        <button
          className={`btn-search-route ${canSearch ? '' : 'btn-search-route--disabled'}`}
          onClick={handleSearch}
          disabled={!canSearch}
        >
          {loading ? '경로 검색 중...' : '경로 검색'}
        </button>

        {error && <div className="route-error">{error}</div>}

        <RouteResult
          routes={routes ?? []}
          selectedRouteId={selectedRouteId}
          onSelectRoute={setSelectedRouteId}
        />
      </div>

      {/* 토글 버튼 그룹 */}
      <div style={{ position: 'fixed', top: '20px', right: '20px', display: 'flex', flexDirection: 'column', gap: '10px', zIndex: 10 }}>
        <button
          style={{ padding: '10px 16px', borderRadius: '8px', border: 'none', cursor: 'pointer', fontWeight: 'bold', fontSize: '13px', color: 'white', minWidth: '110px', boxShadow: '0 2px 6px rgba(0,0,0,0.3)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px', background: showDangerZones ? '#ef4444' : '#6b7280' }}
          onClick={() => setShowDangerZones((v) => !v)}
        >
          <span>🚨 위험구역</span>
          <span style={{ fontSize: '11px' }}>{showDangerZones ? 'ON' : 'OFF'}</span>
        </button>
        <button
          style={{ padding: '10px 16px', borderRadius: '8px', border: 'none', cursor: 'pointer', fontWeight: 'bold', fontSize: '13px', color: 'white', minWidth: '110px', boxShadow: '0 2px 6px rgba(0,0,0,0.3)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px', background: showFacilities ? '#3b82f6' : '#6b7280' }}
          onClick={() => setShowFacilities((v) => !v)}
        >
          <span>📹 시설</span>
          <span style={{ fontSize: '11px' }}>
            {zoom > 3 ? '줌인 필요' : showFacilities ? 'ON' : 'OFF'}
          </span>
        </button>
      </div>

      <RouteOverlay
        map={map}
        routes={routes ?? []}
        selectedRouteId={selectedRouteId}
        dangerZones={dangerZones}
      />

      <DangerZoneOverlay
        map={map}
        zones={dangerZones}
        visible={showDangerZones}
        origin={origin}
        routes={routes ?? []}
        selectedRouteId={selectedRouteId}
      />

      <FacilityOverlay
        map={map}
        visible={showFacilities}
        bounds={bounds}
        zoom={zoom}
      />

      <SOSButton />
    </div>
  );
}
