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
@TableName("health_alert")
public class HealthAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long elderId;

    private Long healthRecordId;

    private Long ruleId;

    private String alertNo;

    private String severity;

    private BigDecimal alertValue;

    private String status;

    private Long assigneeId;

    private LocalDateTime handledAt;

    private String handleResult;

    private LocalDateTime createdAt;
}
