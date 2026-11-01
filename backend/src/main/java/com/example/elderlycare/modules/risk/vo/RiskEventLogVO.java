package com.example.elderlycare.modules.risk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "风险事件流转轨迹日志视图对象")
public class RiskEventLogVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "风险事件ID")
    private Long riskEventId;

    @Schema(description = "动作代码: REPORT, ASSIGN, START_PROCESS, PROGRESS_UPDATE, SUBMIT_REVIEW, REVIEW_REJECT, CLOSE")
    private String action;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "变更前状态")
    private String fromStatus;

    @Schema(description = "变更前状态名称")
    private String fromStatusName;

    @Schema(description = "变更后状态")
    private String toStatus;

    @Schema(description = "变更后状态名称")
    private String toStatusName;

    @Schema(description = "操作人用户ID")
    private Long operatorId;

    @Schema(description = "操作人真实姓名")
    private String operatorName;

    @Schema(description = "处置记录/批注/驳回原因/结论内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作记录时间")
    private LocalDateTime createdAt;
}
