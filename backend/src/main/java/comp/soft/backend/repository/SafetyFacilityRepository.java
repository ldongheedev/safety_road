package comp.soft.backend.repository;

import comp.soft.backend.entity.SafetyFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SafetyFacilityRepository extends JpaRepository<SafetyFacility, Long> {

    List<SafetyFacility> findByFacilityType(String facilityType);

    @Transactional
    void deleteByFacilityTypeAndDataSource(String facilityType, String dataSource);

    @Query(value = "SELECT * FROM safety_facility " +
            "WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius)",
            nativeQuery = true)
    List<SafetyFacility> findWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius
    );

    @Query(value = "SELECT COUNT(*) FROM safety_facility " +
            "WHERE ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))",
            nativeQuery = true)
    int countWithinBounds(
            @Param("lat1") double lat1,
            @Param("lng1") double lng1,
            @Param("lat2") double lat2,
            @Param("lng2") double lng2
    );

    @Query(value = "SELECT COUNT(*) FROM safety_facility " +
            "WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius)",
            nativeQuery = true)
    int countWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius
    );

    @Query(value = "SELECT * FROM safety_facility " +
            "WHERE facility_type = :type " +
            "AND ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326)) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<SafetyFacility> findByTypeWithinBounds(
            @Param("lat1") double lat1,
            @Param("lng1") double lng1,
            @Param("lat2") double lat2,
            @Param("lng2") double lng2,
            @Param("type") String type,
            @Param("limit") int limit
    );

    @Query(value = "SELECT * FROM (" +
            "  SELECT *, ROW_NUMBER() OVER (PARTITION BY facility_type ORDER BY id) AS rn" +
            "  FROM safety_facility" +
            "  WHERE ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))" +
            ") sub ORDER BY rn, facility_type LIMIT :limit",
            nativeQuery = true)
    List<SafetyFacility> findWithinBounds(
            @Param("lat1") double lat1,
            @Param("lng1") double lng1,
            @Param("lat2") double lat2,
            @Param("lng2") double lng2,
            @Param("limit") int limit
    );

    @Query(value = "SELECT COUNT(*) FROM safety_facility " +
            "WHERE facility_type IN ('POLICE_BOX', 'DISTRICT_POLICE') " +
            "AND ST_DWithin(location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius)",
            nativeQuery = true)
    int countPoliceWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius
    );

    @Query(value = "SELECT COUNT(*) FROM safety_facility " +
            "WHERE facility_type IN ('POLICE_BOX', 'DISTRICT_POLICE') " +
            "AND ST_Within(location, ST_MakeEnvelope(:lng1, :lat1, :lng2, :lat2, 4326))",
            nativeQuery = true)
    int countPoliceWithinBounds(
            @Param("lat1") double lat1,
            @Param("lng1") double lng1,
            @Param("lat2") double lat2,
            @Param("lng2") double lng2
    );
}
