import { useState, useRef, useCallback } from 'react';
import './styles/global.css';
import TmapView from './components/TmapView';
import SearchBar from './components/SearchBar';
import RouteOverlay from './components/Map/RouteOverlay';
import RouteResult from './components/Route/RouteResult';
import useRoute from './hooks/useRoute';

export default function App() {
  const [map, setMap] = useState(null);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [selectedRouteId, setSelectedRouteId] = useState(null);
  const originMarkerRef = useRef(null);
  const destMarkerRef = useRef(null);
  const { routes, loading, error, fetchRoutes } = useRoute();

  const handleMapReady = useCallback((mapInstance) => {
    setMap(mapInstance);
  }, []);

  const placeMarker = (markerRef, lat, lng, title, iconUrl) => {
    if (!map) return;
    const Tmapv2 = window.Tmapv2;

    if (markerRef.current) {
      markerRef.current.setMap(null);
    }

    const marker = new Tmapv2.Marker({
      position: new Tmapv2.LatLng(lat, lng),
      icon: iconUrl,
      iconSize: new Tmapv2.Size(36, 44),
      map: map,
      title: title,
    });

    markerRef.current = marker;
    map.setCenter(new Tmapv2.LatLng(lat, lng));
  };

  const handleSelectOrigin = useCallback((lat, lng) => {
    setOrigin({ lat, lng });
    placeMarker(
      originMarkerRef, lat, lng, '출발지',
      'https://tmapapi.tmapmobility.com/upload/tmap/marker/pin_r_m_s.png'
    );
  }, [map]);

  const handleSelectDestination = useCallback((lat, lng) => {
    setDestination({ lat, lng });
    placeMarker(
      destMarkerRef, lat, lng, '목적지',
      'https://tmapapi.tmapmobility.com/upload/tmap/marker/pin_r_m_e.png'
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
      <TmapView onMapReady={handleMapReady} />

      <SearchBar
        onSelectOrigin={handleSelectOrigin}
        onSelectDestination={handleSelectDestination}
      />

      {/* 경로 검색 버튼 */}
      <button
        className={`btn-search-route ${canSearch ? '' : 'btn-search-route--disabled'}`}
        onClick={handleSearch}
        disabled={!canSearch}
      >
        {loading ? '경로 검색 중...' : '경로 검색'}
      </button>

      {error && <div className="route-error">{error}</div>}

      <RouteOverlay
        map={map}
        routes={routes}
        selectedRouteId={selectedRouteId}
      />

      <RouteResult
        routes={routes}
        selectedRouteId={selectedRouteId}
        onSelectRoute={setSelectedRouteId}
      />
    </div>
  );
}
