package com.example.elderlycare.modules.health.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private String metricCode;

    private String metricName;

    private String unit;

    private String valueType;

    private String description;

    private String status;
}
