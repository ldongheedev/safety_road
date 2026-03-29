package com.saferoute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "danger_zone")
@Getter
@Setter
@NoArgsConstructor
public class DangerZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grid_polygon", nullable = false, columnDefinition = "GEOMETRY(Polygon, 4326)")
    private Polygon gridPolygon;

    @Column(name = "safety_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal safetyScore;

    @Column(name = "facility_count")
    private Integer facilityCount = 0;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
