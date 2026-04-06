export default function RouteResult({ routes, selectedRouteId, onSelectRoute }) {
  if (!routes.length) return null;

  return (
    <div className="route-result">
      <div className="route-list">
        {routes.map((route) => {
          const isSelected = route.routeId === selectedRouteId;
          const km = (route.totalDistance / 1000).toFixed(1);
          const min = Math.round(route.totalTime / 60);
          const score = route.safetyScore;
          const level = getLevel(score);

          return (
            <div
              key={route.routeId}
              className={`route-card ${isSelected ? 'route-card--selected' : ''} ${route.recommended ? 'route-card--recommended' : ''}`}
              onClick={() => onSelectRoute(route.routeId)}
            >
              <div className="route-card-header">
                <span className="route-option">{route.searchOption}</span>
                {route.recommended && <span className="route-badge">추천 안전경로</span>}
              </div>

              <div className="route-card-body">
                <span className="route-time">{min}분</span>
                <span className="route-distance">{km}km</span>
              </div>

              {/* 안전 점수 시각화 */}
              <div className="route-safety-section">
                <div className="route-safety-header">
                  <span className={`route-safety-label route-safety-label--${level}`}>
                    {level === 'safe' ? '안전' : level === 'caution' ? '보통' : '주의'}
                  </span>
                  <span className={`route-safety-score route-safety-score--${level}`}>
                    {score != null ? score : '-'}점
                  </span>
                </div>
                <div className="route-safety-bar">
                  <div
                    className={`route-safety-bar-fill route-safety-bar-fill--${level}`}
                    style={{ width: `${score ?? 0}%` }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function getLevel(score) {
  if (score == null) return 'unknown';
  if (score >= 70) return 'safe';
  if (score >= 40) return 'caution';
  return 'danger';
}
