package comp.soft.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DangerZone {
    private Long id;
    private double minLat;
    private double minLng;
    private double maxLat;
    private double maxLng;
    private BigDecimal safetyScore;
    private Integer facilityCount = 0;
    private String riskLevel;
    private LocalDateTime calculatedAt;
}
