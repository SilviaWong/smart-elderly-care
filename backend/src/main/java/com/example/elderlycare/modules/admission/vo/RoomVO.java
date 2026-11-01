package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVO {

    private Long id;

    private Long orgId;

    private Long buildingId;

    private String buildingName;

    private String floorNo;

    private String roomNo;

    private String roomType;

    private String status;

    private Integer bedCount;

    private Integer availableBedCount;

    private LocalDateTime createdAt;
}
