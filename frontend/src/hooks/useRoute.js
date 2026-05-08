import { useState, useCallback } from 'react';
import api from '../api';

export default function useRoute() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchRoutes = useCallback(async (startLat, startLng, endLat, endLng) => {
    setLoading(true);
    setError(null);
    setRoutes([]);
    try {
      const res = await api.get('/routes/safe', {
        params: { startLat, startLng, endLat, endLng },
      });
      if (res.data.length === 0) {
        setError('경로를 찾을 수 없습니다');
      } else {
setRoutes(res.data);
      }
    } catch {
      setError('경로를 찾을 수 없습니다');
    } finally {
      setLoading(false);
    }
  }, []);

  return { routes, loading, error, fetchRoutes };
}
