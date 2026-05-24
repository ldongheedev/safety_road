package comp.soft.backend.repository;

import comp.soft.backend.entity.DangerZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DangerZoneRepository extends JpaRepository<DangerZone, Long> {

    List<DangerZone> findByRiskLevel(String riskLevel);

    @Query(value = "SELECT * FROM danger_zone " +
            "WHERE ST_Intersects(grid_polygon, ST_SetSRID(ST_Point(:lng, :lat), 4326))",
            nativeQuery = true)
    List<DangerZone> findByPoint(
            @Param("lat") double lat,
            @Param("lng") double lng
    );
}
