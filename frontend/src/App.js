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
  const [clickMode, setClickMode] = useState(null);
  const [originLabel, setOriginLabel] = useState(undefined);
  const [destLabel, setDestLabel] = useState(undefined);
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

  const handleMapClick = useCallback((lat, lng) => {
    const label = `선택한 위치 (${lat.toFixed(5)}, ${lng.toFixed(5)})`;
    if (clickMode === 'origin') {
      handleSelectOrigin(lat, lng);
      setOriginLabel(label);
    } else if (clickMode === 'destination') {
      handleSelectDestination(lat, lng);
      setDestLabel(label);
    }
    setClickMode(null);
  }, [clickMode, handleSelectOrigin, handleSelectDestination]);

  const handleSearch = () => {
    if (!origin || !destination) return;
    setSelectedRouteId(null);
fetchRoutes(origin.lat, origin.lng, destination.lat, destination.lng);
  };

  const canSearch = origin && destination && !loading;

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <KakaoMap onMapReady={handleMapReady} onMapClick={handleMapClick} clickMode={clickMode} />

      {/* 좌측 통합 패널 */}
      <div className="side-panel">
        <div style={{ display: 'flex', gap: '6px', marginBottom: '8px' }}>
          <button
            style={{ flex: 1, padding: '8px 4px', borderRadius: '8px', border: 'none', color: 'white', fontWeight: 'bold', fontSize: '12px', cursor: 'pointer', background: clickMode === 'origin' ? '#22c55e' : '#9ca3af' }}
            onClick={() => setClickMode(clickMode === 'origin' ? null : 'origin')}
          >
            {clickMode === 'origin' ? '지도를 클릭하세요...' : '📍 출발지 선택'}
          </button>
          <button
            style={{ flex: 1, padding: '8px 4px', borderRadius: '8px', border: 'none', color: 'white', fontWeight: 'bold', fontSize: '12px', cursor: 'pointer', background: clickMode === 'destination' ? '#3b82f6' : '#9ca3af' }}
            onClick={() => setClickMode(clickMode === 'destination' ? null : 'destination')}
          >
            {clickMode === 'destination' ? '지도를 클릭하세요...' : '📍 도착지 선택'}
          </button>
        </div>

        <SearchBar
          onSelectOrigin={handleSelectOrigin}
          onSelectDestination={handleSelectDestination}
          originValue={originLabel}
          destValue={destLabel}
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

        {/* 토글 버튼 영역 */}
        <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: '10px', display: 'flex', gap: '8px' }}>
          <button
            style={{ flex: 1, padding: '10px', borderRadius: '8px', border: 'none', color: 'white', fontWeight: 'bold', fontSize: '13px', cursor: 'pointer', background: showDangerZones ? '#ef4444' : '#9ca3af' }}
            onClick={() => setShowDangerZones((v) => !v)}
          >
            {`🚨 위험구역 ${showDangerZones ? 'ON' : 'OFF'}`}
          </button>
          <button
            style={{ flex: 1, padding: '10px', borderRadius: '8px', border: 'none', color: 'white', fontWeight: 'bold', fontSize: '13px', cursor: 'pointer', background: showFacilities ? '#3b82f6' : '#9ca3af' }}
            onClick={() => setShowFacilities((v) => !v)}
          >
            {zoom > 6 ? '시설 (줌인 필요)' : `시설 ${showFacilities ? 'ON' : 'OFF'}`}
            {showFacilities && zoom <= 6 && (
              <div style={{ display: 'flex', justifyContent: 'center', gap: '6px', marginTop: '5px' }}>
                {[
                  { color: '#3b82f6', label: 'CCTV' },
                  { color: '#eab308', label: '보안등' },
                  { color: '#1d4ed8', label: '치안센터' },
                  { color: '#7c3aed', label: '지구대' },
                ].map(({ color, label }) => (
                  <div key={label} style={{ display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px' }}>
                    <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: color, border: '1px solid #fff', flexShrink: 0 }} />
                    {label}
                  </div>
                ))}
              </div>
            )}
          </button>
        </div>
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
