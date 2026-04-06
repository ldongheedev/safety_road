import { useState, useRef, useEffect, useCallback } from 'react';
import api from '../api';

export default function SearchBar({ onSelectOrigin, onSelectDestination }) {
  const [originText, setOriginText] = useState('');
  const [destText, setDestText] = useState('');
  const [originResults, setOriginResults] = useState([]);
  const [destResults, setDestResults] = useState([]);
  const [activeField, setActiveField] = useState(null);
  const searchBarRef = useRef(null);

  // 외부 클릭 시 드롭다운 닫기
  useEffect(() => {
    const handleClick = (e) => {
      if (searchBarRef.current && !searchBarRef.current.contains(e.target)) {
        setActiveField(null);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const search = useCallback(async (keyword, setResults) => {
    if (!keyword.trim()) {
      setResults([]);
      return;
    }
    try {
      const res = await api.get('/search/pois', { params: { keyword } });
      setResults(res.data.slice(0, 5));
    } catch {
      setResults([]);
    }
  }, []);

  const handleOriginChange = (e) => {
    const val = e.target.value;
    setOriginText(val);
    setActiveField('origin');
    search(val, setOriginResults);
  };

  const handleDestChange = (e) => {
    const val = e.target.value;
    setDestText(val);
    setActiveField('dest');
    search(val, setDestResults);
  };

  const selectOrigin = (place) => {
    setOriginText(place.name);
    setOriginResults([]);
    setActiveField(null);
    onSelectOrigin(place.lat, place.lng);
  };

  const selectDest = (place) => {
    setDestText(place.name);
    setDestResults([]);
    setActiveField(null);
    onSelectDestination(place.lat, place.lng);
  };

  const handleCurrentLocation = () => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords;
        setOriginText('현재 위치');
        setOriginResults([]);
        setActiveField(null);
        onSelectOrigin(latitude, longitude);
      },
      () => alert('위치 정보를 가져올 수 없습니다.')
    );
  };

  return (
    <div className="search-bar" ref={searchBarRef}>
      {/* 출발지 */}
      <div className="search-field">
        <input
          type="text"
          placeholder="출발지 검색"
          value={originText}
          onChange={handleOriginChange}
          onFocus={() => setActiveField('origin')}
        />
        <button className="btn-current-location" onClick={handleCurrentLocation} title="현재 위치">
          ◎
        </button>
        {activeField === 'origin' && originResults.length > 0 && (
          <ul className="search-dropdown">
            {originResults.map((place, idx) => (
              <li key={idx} onClick={() => selectOrigin(place)}>
                <span className="place-name">{place.name}</span>
                <span className="place-address">{place.address}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* 목적지 */}
      <div className="search-field">
        <input
          type="text"
          placeholder="목적지 검색"
          value={destText}
          onChange={handleDestChange}
          onFocus={() => setActiveField('dest')}
        />
        {activeField === 'dest' && destResults.length > 0 && (
          <ul className="search-dropdown">
            {destResults.map((place, idx) => (
              <li key={idx} onClick={() => selectDest(place)}>
                <span className="place-name">{place.name}</span>
                <span className="place-address">{place.address}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
