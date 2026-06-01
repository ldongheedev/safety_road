import { getLevel, getLevelLabel } from '../../utils/safetyUtils';

export default function RouteResult({ routes, selectedRouteId, onSelectRoute }) {
  if (!routes.length) return null;

  const recommendedRoute = routes.find(r => r.recommended);

  return (
    <div className="route-result">
      <button
        onClick={() => recommendedRoute && onSelectRoute(recommendedRoute.routeId)}
        disabled={!recommendedRoute}
        style={{
          width: '100%',
          background: recommendedRoute ? '#22c55e' : '#9ca3af',
          color: '#fff',
          borderRadius: '8px',
          padding: '12px',
          fontWeight: 'bold',
          border: 'none',
          cursor: recommendedRoute ? 'pointer' : 'not-allowed',
          marginBottom: '8px',
        }}
        onMouseEnter={e => { if (recommendedRoute) e.currentTarget.style.background = '#16a34a'; }}
        onMouseLeave={e => { if (recommendedRoute) e.currentTarget.style.background = '#22c55e'; }}
      >
        {recommendedRoute ? '🛡️ 가장 안전한 경로로 안내' : '추천 경로 없음'}
      </button>
      <div className="route-list">
        {routes.map((route) => {
          const isSelected = route.routeId === selectedRouteId;
          const km = (route.totalDistance / 1000).toFixed(1);
          const min = Math.round(route.totalTime / 60);

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
              {route.safetyScore != null && (
                <div className="route-safety-section">
                  <div className="route-safety-header">
                    <span className={`route-safety-label route-safety-label--${getLevel(route.safetyScore)}`}>
                      {getLevelLabel(route.safetyScore)}
                    </span>
                    <span className={`route-safety-score route-safety-score--${getLevel(route.safetyScore)}`}>
                      {route.safetyScore.toFixed(1)}점
                    </span>
                  </div>
                  <div className="route-safety-bar">
                    <div
                      className={`route-safety-bar-fill route-safety-bar-fill--${getLevel(route.safetyScore)}`}
                      style={{ width: `${route.safetyScore}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

