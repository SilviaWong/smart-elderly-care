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
 * 风险事件流转轨迹记录实体 (risk_event_log)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("risk_event_log")
public class RiskEventLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long riskEventId;

    private String action;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String content;

    private LocalDateTime createdAt;
}
