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
        // 백엔드 safetyScore 미구현 시 mock 데이터 주입
        const scored = res.data.map((route, i) => ({
          ...route,
          safetyScore: route.safetyScore ?? [82, 65, 47][i % 3],
          recommended: false,
        }));
        // 가장 안전한 경로를 추천으로 설정
        const bestIdx = scored.reduce(
          (best, r, idx) => (r.safetyScore > scored[best].safetyScore ? idx : best),
          0
        );
        scored[bestIdx].recommended = true;
        setRoutes(scored);
      }
    } catch {
      setError('경로를 찾을 수 없습니다');
    } finally {
      setLoading(false);
    }
  }, []);

  return { routes, loading, error, fetchRoutes };
}
