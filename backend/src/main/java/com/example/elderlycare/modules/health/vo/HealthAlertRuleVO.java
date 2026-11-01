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
public class HealthAlertRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private Long metricId;

    private String metricCode;

    private String metricName;

    private String unit;

    private String ruleName;

    private String conditionType;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private String severity;

    private Integer dedupMinutes;

    private Integer enabled;

    private LocalDateTime createdAt;
}
