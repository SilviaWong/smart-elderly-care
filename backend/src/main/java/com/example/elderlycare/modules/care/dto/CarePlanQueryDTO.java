package com.example.elderlycare.modules.care.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanQueryDTO {

    private String planNo;

    private Long elderId;

    private String elderName;

    private Long careLevelId;

    private String status;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
