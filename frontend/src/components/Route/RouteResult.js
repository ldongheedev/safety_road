export default function RouteResult({ routes, selectedRouteId, onSelectRoute }) {
  if (!routes.length) return null;

  return (
    <div className="route-result">
      <div className="route-list">
        {routes.map((route) => {
          const isSelected = route.routeId === selectedRouteId;
          const km = (route.totalDistance / 1000).toFixed(1);
          const min = Math.round(route.totalTime / 60);

          return (
            <div
              key={route.routeId}
              className={`route-card ${isSelected ? 'route-card--selected' : ''}`}
              onClick={() => onSelectRoute(route.routeId)}
            >
              <div className="route-card-header">
                <span className="route-option">{route.searchOption}</span>
                {route.recommended && <span className="route-badge">추천</span>}
              </div>
              <div className="route-card-body">
                <span className="route-time">{min}분</span>
                <span className="route-distance">{km}km</span>
              </div>
              <div className="route-card-footer">
                <span className="route-safety">
                  안전점수: {route.safetyScore != null ? route.safetyScore : '-'}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
