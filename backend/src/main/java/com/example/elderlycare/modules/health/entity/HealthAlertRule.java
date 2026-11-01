package com.example.elderlycare.modules.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("health_alert_rule")
public class HealthAlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long metricId;

    private String ruleName;

    private String conditionType;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private String severity;

    private Integer dedupMinutes;

    private Integer enabled;

    private LocalDateTime createdAt;
}
