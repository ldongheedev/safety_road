import { useEffect, useState } from 'react';

const DURATION = 10000;

export default function DangerAlert({ zone, onDismiss }) {
  const [progress, setProgress] = useState(100);

  useEffect(() => {
    const start = Date.now();
    const timer = setTimeout(onDismiss, DURATION);

    const interval = setInterval(() => {
      const elapsed = Date.now() - start;
      const remaining = Math.max(0, 100 - (elapsed / DURATION) * 100);
      setProgress(remaining);
    }, 50);

    return () => {
      clearTimeout(timer);
      clearInterval(interval);
    };
  }, [onDismiss]);

  return (
    <div className="danger-alert">
      <span className="danger-alert-icon">⚠️</span>
      <div className="danger-alert-text">
        <span className="danger-alert-title">위험 구역 진입</span>
        <span className="danger-alert-desc">
          안전 점수 {zone.safetyScore != null ? Math.round(zone.safetyScore) : '?'}점 — 주변을 주의하세요
        </span>
        <div className="danger-alert-gauge-bg">
          <div className="danger-alert-gauge-fill" style={{ width: `${progress}%` }} />
        </div>
      </div>
      <button className="danger-alert-close" onClick={onDismiss}>✕</button>
    </div>
  );
}
