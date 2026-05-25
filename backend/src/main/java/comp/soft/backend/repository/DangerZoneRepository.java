package comp.soft.backend.repository;

import comp.soft.backend.entity.DangerZone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class DangerZoneRepository {

    private final JdbcTemplate jdbcTemplate;

    public DangerZoneRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<DangerZone> rowMapper = (rs, rowNum) -> {
        DangerZone zone = new DangerZone();
        zone.setId(rs.getLong("id"));
        zone.setMinLat(rs.getDouble("min_lat"));
        zone.setMinLng(rs.getDouble("min_lng"));
        zone.setMaxLat(rs.getDouble("max_lat"));
        zone.setMaxLng(rs.getDouble("max_lng"));
        zone.setSafetyScore(rs.getBigDecimal("safety_score"));
        zone.setFacilityCount(rs.getInt("facility_count"));
        zone.setRiskLevel(rs.getString("risk_level"));
        Timestamp ts = rs.getTimestamp("calculated_at");
        if (ts != null) zone.setCalculatedAt(ts.toLocalDateTime());
        return zone;
    };

    private static final String SELECT_BOUNDS =
            "SELECT id, " +
            "ST_YMin(grid_polygon) AS min_lat, ST_XMin(grid_polygon) AS min_lng, " +
            "ST_YMax(grid_polygon) AS max_lat, ST_XMax(grid_polygon) AS max_lng, " +
            "safety_score, facility_count, risk_level, calculated_at " +
            "FROM danger_zone ";

    public List<DangerZone> findAll() {
        return jdbcTemplate.query(SELECT_BOUNDS, rowMapper);
    }

    public List<DangerZone> findByRiskLevel(String riskLevel) {
        return jdbcTemplate.query(SELECT_BOUNDS + "WHERE risk_level = ?", rowMapper, riskLevel);
    }

    public List<DangerZone> findByPoint(double lat, double lng) {
        return jdbcTemplate.query(
                SELECT_BOUNDS + "WHERE ST_Intersects(grid_polygon, ST_SetSRID(ST_Point(?, ?), 4326))",
                rowMapper, lng, lat);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM danger_zone");
    }

    public void saveAll(List<DangerZone> zones) {
        String sql = "INSERT INTO danger_zone (grid_polygon, safety_score, facility_count, risk_level, calculated_at) " +
                     "VALUES (ST_MakeEnvelope(?, ?, ?, ?, 4326), ?, ?, ?, NOW())";
        jdbcTemplate.batchUpdate(sql, zones, zones.size(), (ps, zone) -> {
            ps.setDouble(1, zone.getMinLng());
            ps.setDouble(2, zone.getMinLat());
            ps.setDouble(3, zone.getMaxLng());
            ps.setDouble(4, zone.getMaxLat());
            ps.setBigDecimal(5, zone.getSafetyScore());
            ps.setInt(6, zone.getFacilityCount());
            ps.setString(7, zone.getRiskLevel());
        });
    }
}