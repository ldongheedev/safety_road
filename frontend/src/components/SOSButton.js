import React, { useState, useEffect, useRef, useCallback } from 'react';

const COUNTDOWN_SECONDS = 10;

export default function SOSButton() {
  const [active, setActive] = useState(false);
  const [count, setCount] = useState(COUNTDOWN_SECONDS);
  const timerRef = useRef(null);

  const cancel = useCallback(() => {
    clearInterval(timerRef.current);
    timerRef.current = null;
    setActive(false);
    setCount(COUNTDOWN_SECONDS);
  }, []);

  const callEmergency = useCallback(() => {
    clearInterval(timerRef.current);
    timerRef.current = null;
    window.location.href = 'tel:112';
  }, []);

  const start = () => {
    setActive(true);
    setCount(COUNTDOWN_SECONDS);
  };

  useEffect(() => {
    if (!active) return;

    timerRef.current = setInterval(() => {
      setCount((prev) => {
        if (prev <= 1) {
          callEmergency();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timerRef.current);
  }, [active, callEmergency]);

  if (!active) {
    return (
      <button className="sos-fab" onClick={start}>
        SOS
      </button>
    );
  }

  return (
    <div className="sos-overlay">
      <div className="sos-modal">
        <p className="sos-title">긴급 신고</p>
        <p className="sos-desc">
          <span className="sos-count">{count}</span>초 후 112에 자동 연결됩니다
        </p>

        <div className="sos-progress-ring">
          <svg viewBox="0 0 120 120">
            <circle className="sos-ring-bg" cx="60" cy="60" r="52" />
            <circle
              className="sos-ring-fg"
              cx="60"
              cy="60"
              r="52"
              style={{
                strokeDashoffset:
                  2 * Math.PI * 52 * (1 - count / COUNTDOWN_SECONDS),
              }}
            />
          </svg>
          <span className="sos-ring-number">{count}</span>
        </div>

        <div className="sos-actions">
          <button className="sos-btn-cancel" onClick={cancel}>
            취소
          </button>
          <button className="sos-btn-now" onClick={callEmergency}>
            즉시 신고
          </button>
        </div>
      </div>
    </div>
  );
}