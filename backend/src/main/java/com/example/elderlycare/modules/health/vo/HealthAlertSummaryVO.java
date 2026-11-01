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
public class HealthAlertSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalAlerts;

    private Integer openCount;

    private Integer processingCount;

    private Integer resolvedCount;

    private Integer ignoredCount;

    private Integer criticalCount;
}
