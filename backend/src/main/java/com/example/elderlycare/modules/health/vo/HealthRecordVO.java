package com.example.elderlycare.modules.health.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedInfo;

    private Long metricId;

    private String metricCode;

    private String metricName;

    private BigDecimal metricValue;

    private String unitSnapshot;

    private LocalDateTime measuredAt;

    private String sourceType;

    private Long recorderId;

    private String recorderName;

    private String remark;

    private LocalDateTime createdAt;
}
