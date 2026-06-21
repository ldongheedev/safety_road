import { useState, useRef, useCallback, useEffect } from 'react';
import './styles/global.css';
import KakaoMap from './components/KakaoMap';
import SearchBar from './components/SearchBar';
import RouteOverlay from './components/Map/RouteOverlay';
import FacilityOverlay from './components/Map/FacilityOverlay';
import LocationOverlay from './components/Map/LocationOverlay';
import RouteResult from './components/Route/RouteResult';
import useRoute from './hooks/useRoute';
import useCurrentLocation from './hooks/useCurrentLocation';
import SOSButton from './components/SOSButton';
import DangerAlert from './components/DangerAlert';

export default function App() {
  const [map, setMap] = useState(null);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [selectedRouteId, setSelectedRouteId] = useState(null);
  const [showFacilities, setShowFacilities] = useState(false);
  const [clickMode, setClickMode] = useState(null);
  const [originLabel, setOriginLabel] = useState(undefined);
  const [destLabel, setDestLabel] = useState(undefined);
  const [bounds, setBounds] = useState(null);
  const [zoom, setZoom] = useState(null);
  const [dangerZones, setDangerZones] = useState([]);
  const [alertZone, setAlertZone] = useState(null);
  const [panelCollapsed, setPanelCollapsed] = useState(false);
  const [resultsMode, setResultsMode] = useState(false);
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= 768);

  const panelRef = useRef(null);
  const panelHandleRef = useRef(null);
  const dragStartY = useRef(null);
  const dragStartHeight = useRef(null);

  const originMarkerRef = useRef(null);
  const destMarkerRef = useRef(null);
  const boundsTimerRef = useRef(null);
  const initialCenteredRef = useRef(false);
  const lastAlertTimeRef = useRef(0);
  const prevInDangerRef = useRef(false);

  const { routes, loading, error, fetchRoutes, clearRoutes } = useRoute();
  const { location: currentLocation, error: locationError } = useCurrentLocation();

  // 화면 크기 감지
  useEffect(() => {
    const update = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, []);

  // 위험구역 데이터 한 번만 로드
  useEffect(() => {
    fetch('/api/danger-zones')
      .then((r) => r.json())
      .then((data) => {
        if (Array.isArray(data)) {
          setDangerZones(data.filter((z) => z.riskLevel === 'HIGH'));
        }
      })
      .catch(() => {});
  }, []);

  // 추천 경로 자동 선택 + 결과 뷰 전환
  useEffect(() => {
    if (!routes || routes.length === 0) return;
    const recommended = routes.find((r) => r.recommended);
    if (recommended) setSelectedRouteId(recommended.routeId);
    setResultsMode(true);
    setPanelCollapsed(false);
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

    update();
    kakao.maps.event.addListener(map, 'bounds_changed', update);
    kakao.maps.event.addListener(map, 'zoom_changed', update);

    return () => {
      clearTimeout(boundsTimerRef.current);
      kakao.maps.event.removeListener(map, 'bounds_changed', update);
      kakao.maps.event.removeListener(map, 'zoom_changed', update);
    };
  }, [map]);

  // 첫 위치 수신 시 지도 중심 이동
  useEffect(() => {
    if (!map || !currentLocation || initialCenteredRef.current) return;
    const kakao = window.kakao;
    map.setCenter(new kakao.maps.LatLng(currentLocation.lat, currentLocation.lng));
    initialCenteredRef.current = true;
  }, [map, currentLocation]);

  // 위험구역 진입 감지
  useEffect(() => {
    if (!currentLocation || !dangerZones.length) return;

    const inDangerZone = dangerZones.find((zone) => {
      const [minLat, minLng] = zone.bounds[0];
      const [maxLat, maxLng] = zone.bounds[2];
      return (
        currentLocation.lat >= minLat && currentLocation.lat <= maxLat &&
        currentLocation.lng >= minLng && currentLocation.lng <= maxLng
      );
    });

    const now = Date.now();
    const wasInDanger = prevInDangerRef.current;

    if (inDangerZone && !wasInDanger && now - lastAlertTimeRef.current > 30000) {
      setAlertZone(inDangerZone);
      lastAlertTimeRef.current = now;
    }

    prevInDangerRef.current = !!inDangerZone;
  }, [currentLocation, dangerZones]);

  const handleMapReady = useCallback((mapInstance) => {
    setMap(mapInstance);
  }, []);

  const placeMarker = (markerRef, lat, lng, title, imageSrc) => {
    if (!map) return;
    const kakao = window.kakao;
    if (markerRef.current) markerRef.current.setMap(null);

    const markerImage = new kakao.maps.MarkerImage(imageSrc, new kakao.maps.Size(36, 44));
    const marker = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(lat, lng),
      image: markerImage,
      title,
    });
    marker.setMap(map);
    markerRef.current = marker;
    map.setCenter(new kakao.maps.LatLng(lat, lng));
  };

  const handleSelectOrigin = useCallback((lat, lng, label) => {
    setOrigin({ lat, lng });
    if (label) setOriginLabel(label);
    placeMarker(
      originMarkerRef, lat, lng, '출발지',
      'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/red_b.png'
    );
  }, [map]);

  const handleSelectDestination = useCallback((lat, lng, label) => {
    setDestination({ lat, lng });
    if (label) setDestLabel(label);
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
    setResultsMode(false);
    fetchRoutes(origin.lat, origin.lng, destination.lat, destination.lng);
  };

  const handleReSearch = () => {
    setResultsMode(false);
    setSelectedRouteId(null);
    clearRoutes();
    // 지도 위 출발/도착 마커 제거
    if (originMarkerRef.current) { originMarkerRef.current.setMap(null); originMarkerRef.current = null; }
    if (destMarkerRef.current)   { destMarkerRef.current.setMap(null);   destMarkerRef.current = null; }
    setOrigin(null);
    setDestination(null);
    setOriginLabel(undefined);
    setDestLabel(undefined);
  };

  const handleDragStart = (e) => {
    dragStartY.current = e.touches[0].clientY;
    dragStartHeight.current = panelRef.current?.getBoundingClientRect().height ?? 0;
    if (panelRef.current) panelRef.current.style.transition = 'none';
  };

  const handleDragEnd = (e) => {
    if (dragStartY.current === null) return;
    const delta = e.changedTouches[0].clientY - dragStartY.current;
    if (panelRef.current) {
      panelRef.current.style.transition = '';
      panelRef.current.style.maxHeight = '';
    }
    if (delta > 60) setPanelCollapsed(true);
    else if (delta < -60) setPanelCollapsed(false);
    dragStartY.current = null;
  };

  // non-passive로 등록해야 preventDefault()가 작동 → 지도 이동 차단
  useEffect(() => {
    const handle = panelHandleRef.current;
    if (!handle) return;

    const onTouchMove = (e) => {
      if (dragStartY.current === null) return;
      e.preventDefault();
      const delta = e.touches[0].clientY - dragStartY.current;
      const newHeight = Math.min(
        Math.max(44, dragStartHeight.current - delta),
        window.innerHeight * 0.62
      );
      if (panelRef.current) panelRef.current.style.maxHeight = `${newHeight}px`;
    };

    handle.addEventListener('touchmove', onTouchMove, { passive: false });
    return () => handle.removeEventListener('touchmove', onTouchMove);
  }, []);

  const handleCenterOnMe = () => {
    if (!map || !currentLocation) {
      if (locationError) alert('위치 권한이 거부되었거나 HTTPS 환경이 아닙니다.\n브라우저 설정에서 위치 권한을 허용해주세요.');
      return;
    }
    const kakao = window.kakao;
    map.setCenter(new kakao.maps.LatLng(currentLocation.lat, currentLocation.lng));
    map.setLevel(3);
  };

  const canSearch = origin && destination && !loading;

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <KakaoMap onMapReady={handleMapReady} onMapClick={handleMapClick} clickMode={clickMode} />

      {/* 현재 위치로 이동 버튼 */}
      <button
        className={`btn-my-location${!currentLocation ? ' btn-my-location--inactive' : ''}`}
        style={{ bottom: (isMobile && resultsMode) ? 'calc(80vh + 12px)' : undefined }}
        onClick={handleCenterOnMe}
        title={locationError ? '위치 권한이 필요합니다' : currentLocation ? '내 위치로' : '위치 확인 중...'}
      >
        {locationError ? '🚫' : currentLocation ? '📍' : '⏳'}
      </button>

      {/* 위험구역 진입 알림 */}
      {alertZone && (
        <DangerAlert zone={alertZone} onDismiss={() => setAlertZone(null)} />
      )}

      {/* 좌측/하단 통합 패널 */}
      <div
        ref={panelRef}
        className={`side-panel${panelCollapsed ? ' side-panel--collapsed' : ''}${resultsMode ? ' side-panel--results' : ''}`}
      >
        <div
          ref={panelHandleRef}
          className="panel-handle"
          onClick={() => setPanelCollapsed(v => !v)}
          onTouchStart={handleDragStart}
          onTouchEnd={handleDragEnd}
        >
          <span className="panel-handle-bar" />
          <span className="panel-handle-chevron">{panelCollapsed ? '▲' : '▼'}</span>
        </div>

        {(isMobile && resultsMode) ? (
          /* ── 모바일 결과 뷰 ── */
          <>
            <div className="results-header">
              <div className="results-route-summary">
                <span className="results-point results-point--origin">{originLabel || '출발지'}</span>
                <span className="results-arrow">→</span>
                <span className="results-point results-point--dest">{destLabel || '목적지'}</span>
              </div>
              <button className="btn-re-search" onClick={handleReSearch}>재검색</button>
            </div>
            {error && <div className="route-error">{error}</div>}
            <RouteResult
              routes={routes ?? []}
              selectedRouteId={selectedRouteId}
              onSelectRoute={setSelectedRouteId}
            />
          </>
        ) : (
          /* ── 검색 뷰 (데스크톱 항상 / 모바일 검색 전) ── */
          <>
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

            {/* 데스크톱: 검색 결과를 폼 아래에 바로 표시 */}
            <RouteResult
              routes={routes ?? []}
              selectedRouteId={selectedRouteId}
              onSelectRoute={setSelectedRouteId}
            />

            {/* 토글 버튼 영역 */}
            <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: '10px' }}>
              <button
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: 'none', color: 'white', fontWeight: 'bold', fontSize: '13px', cursor: 'pointer', background: showFacilities ? '#3b82f6' : '#9ca3af' }}
                onClick={() => setShowFacilities((v) => !v)}
              >
                {zoom > 5 ? '시설 (줌인 필요)' : `시설 ${showFacilities ? 'ON' : 'OFF'}`}
                {showFacilities && zoom <= 5 && (
                  <div style={{ display: 'flex', justifyContent: 'center', gap: '6px', marginTop: '5px' }}>
                    {[
                      { color: '#3b82f6', label: 'CCTV' },
                      { color: '#eab308', label: '보안등' },
                      { color: '#ef4444', label: '경찰/치안' },
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
          </>
        )}
      </div>

      <RouteOverlay map={map} routes={routes ?? []} selectedRouteId={selectedRouteId} />
      <FacilityOverlay map={map} visible={showFacilities} bounds={bounds} zoom={zoom} />
      <LocationOverlay map={map} location={currentLocation} />
      <SOSButton />
    </div>
  );
}
