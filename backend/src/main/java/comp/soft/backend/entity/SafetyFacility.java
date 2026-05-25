package comp.soft.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SafetyFacility {
    private Long id;
    private String facilityType;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String dataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}