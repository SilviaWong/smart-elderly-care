package com.example.elderlycare.modules.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "风险事件工作台统计看板视图对象")
public class RiskEventSummaryVO {

    @Schema(description = "机构风险事件总数")
    private Long totalCount;

    @Schema(description = "待分派事件数 (REPORTED)")
    private Long reportedCount;

    @Schema(description = "已分派事件数 (ASSIGNED)")
    private Long assignedCount;

    @Schema(description = "处置中事件数 (PROCESSING)")
    private Long processingCount;

    @Schema(description = "待主管复核事件数 (PENDING_REVIEW)")
    private Long pendingReviewCount;

    @Schema(description = "已关闭结案数 (CLOSED)")
    private Long closedCount;

    @Schema(description = "高风险及危急事件总数 (HIGH + CRITICAL)")
    private Long highCriticalCount;
}
