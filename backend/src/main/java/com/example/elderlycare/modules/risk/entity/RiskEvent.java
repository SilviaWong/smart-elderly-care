package com.example.elderlycare.modules.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全风险事件实体 (risk_event)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("risk_event")
public class RiskEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String eventNo;

    private Long elderId;

    private String eventType;

    private String severity;

    private LocalDateTime occurredAt;

    private String locationDesc;

    private String description;

    private Long reporterId;

    private Long assigneeId;

    private String status;

    private String conclusion;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;
}
