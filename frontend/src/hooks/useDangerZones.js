import { useState, useEffect } from 'react';
import api from '../api';

export default function useDangerZones() {
  const [zones, setZones] = useState([]);

  useEffect(() => {
    api.get('/danger-zones')
      .then((res) => setZones(res.data))
      .catch(() => setZones([]));
  }, []);

  return zones;
}
