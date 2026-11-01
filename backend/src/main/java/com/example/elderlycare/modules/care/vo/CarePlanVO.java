package com.example.elderlycare.modules.care.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private String planNo;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedInfo;

    private Long careLevelId;

    private String careLevelName;

    private String careLevelCode;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private Long creatorId;

    private String creatorName;

    private String remark;

    private Integer itemCount;

    private LocalDateTime createdAt;
}
