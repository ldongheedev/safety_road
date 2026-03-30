import { useState, useRef, useCallback } from 'react';
import './styles/global.css';
import KakaoMap from './components/KakaoMap';
import SearchBar from './components/SearchBar';
import RouteOverlay from './components/Map/RouteOverlay';
import DangerZoneOverlay from './components/Map/DangerZoneOverlay';
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
  const originMarkerRef = useRef(null);
  const destMarkerRef = useRef(null);
  const { routes, loading, error, fetchRoutes } = useRoute();
  const dangerZones = useDangerZones();

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
          routes={routes}
          selectedRouteId={selectedRouteId}
          onSelectRoute={setSelectedRouteId}
        />
      </div>

      {/* 위험구역 토글 버튼 */}
      <button
        className={`btn-danger-toggle ${showDangerZones ? 'btn-danger-toggle--active' : ''}`}
        onClick={() => setShowDangerZones((v) => !v)}
      >
        {showDangerZones ? '위험구역 ON' : '위험구역 OFF'}
      </button>

      <RouteOverlay
        map={map}
        routes={routes}
        selectedRouteId={selectedRouteId}
      />

      <DangerZoneOverlay
        map={map}
        zones={dangerZones}
        visible={showDangerZones}
      />

      <SOSButton />
    </div>
  );
}
