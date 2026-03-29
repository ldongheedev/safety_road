package com.saferoute.repository;

import com.saferoute.entity.SafetyFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SafetyFacilityRepository extends JpaRepository<SafetyFacility, Long> {

    List<SafetyFacility> findByFacilityType(String facilityType);

    @Query(value = "SELECT * FROM safety_facility " +
            "WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius)",
            nativeQuery = true)
    List<SafetyFacility> findWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius
    );
}
