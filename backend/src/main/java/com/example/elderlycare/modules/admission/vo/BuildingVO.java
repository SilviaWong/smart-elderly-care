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
public class BuildingVO {

    private Long id;

    private Long orgId;

    private String buildingName;

    private String buildingCode;

    private String address;

    private String status;

    private Integer roomCount;

    private Integer bedCount;

    private LocalDateTime createdAt;
}
