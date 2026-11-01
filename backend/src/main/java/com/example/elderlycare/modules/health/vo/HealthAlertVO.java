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
public class HealthAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private String alertNo;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedInfo;

    private Long healthRecordId;

    private Long metricId;

    private String metricCode;

    private String metricName;

    private String unit;

    private Long ruleId;

    private String ruleName;

    private String conditionType;

    private BigDecimal thresholdMin;

    private BigDecimal thresholdMax;

    private String severity;

    private BigDecimal alertValue;

    private String status;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime handledAt;

    private String handleResult;

    private LocalDateTime createdAt;
}
