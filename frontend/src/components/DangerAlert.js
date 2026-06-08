import { useEffect } from 'react';

export default function DangerAlert({ zone, onDismiss }) {
  useEffect(() => {
    const timer = setTimeout(onDismiss, 6000);
    return () => clearTimeout(timer);
  }, [onDismiss]);

  return (
    <div className="danger-alert">
      <span className="danger-alert-icon">⚠️</span>
      <div className="danger-alert-text">
        <span className="danger-alert-title">위험 구역 진입</span>
        <span className="danger-alert-desc">
          안전 점수 {zone.safetyScore != null ? Math.round(zone.safetyScore) : '?'}점 — 주변을 주의하세요
        </span>
      </div>
      <button className="danger-alert-close" onClick={onDismiss}>✕</button>
    </div>
  );
}
