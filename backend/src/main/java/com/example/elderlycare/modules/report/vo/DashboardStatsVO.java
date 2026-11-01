package com.example.elderlycare.modules.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "运营数据全景看板统计指标")
public class DashboardStatsVO {

    @Schema(description = "在住在册长者人数")
    private Long activeElderCount;

    @Schema(description = "机构床位总数")
    private Long totalBedCount;

    @Schema(description = "已占用入住床位数")
    private Long occupiedBedCount;

    @Schema(description = "床位使用率百分比 (0.00% ~ 100.00%)")
    private BigDecimal bedOccupancyRate;

    @Schema(description = "待派单/待处理服务工单数")
    private Long pendingOrderCount;

    @Schema(description = "今日护理任务完成率百分比 (0.00% ~ 100.00%)")
    private BigDecimal careCompletionRate;

    @Schema(description = "待处理健康体征预警数")
    private Long healthAlertCount;

    @Schema(description = "当月实际收款到账总额 (元)")
    private BigDecimal currentMonthRevenue;

    @Schema(description = "待处置安全风险事件数")
    private Long openRiskEventCount;

    @Schema(description = "今日物联网设备遥测点总数")
    private Long todayTelemetryCount;

    @Schema(description = "近7日入住与退住流转趋势")
    private List<DailyTrendItem> admissionTrend;

    @Schema(description = "近7日护理任务计划与完成趋势")
    private List<CareTaskTrendItem> careTaskTrend;

    @Schema(description = "当前未关闭安全风险事件等级分布")
    private Map<String, Long> riskDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendItem {
        private String date; // YYYY-MM-DD
        private Long checkInCount;
        private Long dischargeCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareTaskTrendItem {
        private String date;
        private Long totalCount;
        private Long completedCount;
    }
}
