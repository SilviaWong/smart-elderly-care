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
public class HealthTrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDateTime measuredAt;

    private BigDecimal metricValue;

    private String unitSnapshot;

    private String sourceType;
}
