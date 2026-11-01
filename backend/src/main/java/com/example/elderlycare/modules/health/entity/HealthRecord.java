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
@TableName("health_record")
public class HealthRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long elderId;

    private Long metricId;

    private BigDecimal metricValue;

    private String unitSnapshot;

    private LocalDateTime measuredAt;

    private String sourceType;

    private Long sourceDeviceId;

    private Long recorderId;

    private String remark;

    private LocalDateTime createdAt;
}
