package comp.soft.backend.repository;

import comp.soft.backend.entity.SafetyFacility;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class SafetyFacilityRepository {

    private final JdbcTemplate jdbcTemplate;

    public SafetyFacilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SafetyFacility> rowMapper = (rs, rowNum) -> {
        SafetyFacility f = new SafetyFacility();
        f.setId(rs.getLong("id"));
        f.setFacilityType(rs.getString("facility_type"));
        f.setName(rs.getString("name"));
        f.setAddress(rs.getString("address"));
        f.setLatitude(rs.getDouble("latitude"));
        f.setLongitude(rs.getDouble("longitude"));
        f.setDataSource(rs.getString("data_source"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) f.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) f.setUpdatedAt(updated.toLocalDateTime());
        return f;
    };

    private static final String SELECT_COORDS =
            "SELECT id, facility_type, name, address, data_source, created_at, updated_at, " +
            "ST_Y(location) AS latitude, ST_X(location) AS longitude " +
            "FROM safety_facility ";

    public List<SafetyFacility> findByFacilityType(String facilityType) {
        return jdbcTemplate.query(SELECT_COORDS + "WHERE facility_type = ?", rowMapper, facilityType);
    }

    @Transactional
    public void deleteByFacilityTypeAndDataSource(String facilityType, String dataSource) {
        jdbcTemplate.update(
                "DELETE FROM safety_facility WHERE facility_type = ? AND data_source = ?",
                facilityType, dataSource);
    }

    public List<SafetyFacility> findWithinRadius(double lat, double lng, double radius) {
        return jdbcTemplate.query(
                SELECT_COORDS + "WHERE ST_DWithin(location, ST_SetSRID(ST_Point(?, ?), 4326), ?)",
                rowMapper, lng, lat, radius);
    }

    public int countWithinRadius(double lat, double lng, double radius) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safety_facility " +
                "WHERE ST_DWithin(location, ST_SetSRID(ST_Point(?, ?), 4326), ?)",
                Integer.class, lng, lat, radius);
        return count != null ? count : 0;
    }

    public int countWithinBounds(double lat1, double lng1, double lat2, double lng2) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safety_facility " +
                "WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))",
                Integer.class, lng1, lat1, lng2, lat2);
        return count != null ? count : 0;
    }

    public int countPoliceWithinRadius(double lat, double lng, double radius) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safety_facility " +
                "WHERE facility_type IN ('POLICE_BOX', 'DISTRICT_POLICE') " +
                "AND ST_DWithin(location, ST_SetSRID(ST_Point(?, ?), 4326), ?)",
                Integer.class, lng, lat, radius);
        return count != null ? count : 0;
    }

    public int countPoliceWithinBounds(double lat1, double lng1, double lat2, double lng2) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safety_facility " +
                "WHERE facility_type IN ('POLICE_BOX', 'DISTRICT_POLICE') " +
                "AND ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))",
                Integer.class, lng1, lat1, lng2, lat2);
        return count != null ? count : 0;
    }

    public List<SafetyFacility> findByTypeWithinBounds(double lat1, double lng1, double lat2, double lng2,
                                                        String type, int limit) {
        return jdbcTemplate.query(
                SELECT_COORDS +
                "WHERE facility_type = ? " +
                "AND ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326)) " +
                "LIMIT ?",
                rowMapper, type, lng1, lat1, lng2, lat2, limit);
    }

    public List<SafetyFacility> findWithinBounds(double lat1, double lng1, double lat2, double lng2, int limit) {
        return jdbcTemplate.query(
                "SELECT id, facility_type, name, address, data_source, created_at, updated_at, " +
                "ST_Y(location) AS latitude, ST_X(location) AS longitude " +
                "FROM (" +
                "  SELECT *, ROW_NUMBER() OVER (PARTITION BY facility_type ORDER BY id) AS rn" +
                "  FROM safety_facility" +
                "  WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))" +
                ") sub ORDER BY rn, facility_type LIMIT ?",
                rowMapper, lng1, lat1, lng2, lat2, limit);
    }

    public void saveAll(List<SafetyFacility> facilities) {
        String sql = "INSERT INTO safety_facility (facility_type, name, address, location, data_source, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ST_SetSRID(ST_Point(?, ?), 4326), ?, NOW(), NOW())";
        jdbcTemplate.batchUpdate(sql, facilities, facilities.size(), (ps, f) -> {
            ps.setString(1, f.getFacilityType());
            ps.setString(2, f.getName());
            ps.setString(3, f.getAddress());
            ps.setDouble(4, f.getLongitude());
            ps.setDouble(5, f.getLatitude());
            ps.setString(6, f.getDataSource());
        });
    }
}