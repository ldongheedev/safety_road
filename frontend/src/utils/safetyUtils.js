export function getLevel(score) {
  if (score == null) return 'unknown';
  if (score >= 71) return 'safe';
  if (score >= 41) return 'caution';
  return 'danger';
}

export function getLevelColor(score) {
  if (score == null) return '#6b7280';
  if (score >= 71) return '#22c55e';
  if (score >= 41) return '#f97316';
  return '#ef4444';
}

export function getLevelLabel(score) {
  if (score == null) return '-';
  if (score >= 71) return '안전';
  if (score >= 41) return '보통';
  return '위험';
}
