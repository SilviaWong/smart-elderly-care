package com.example.elderlycare.modules.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "综合业务报表数据查询参数")
public class ReportQueryDTO {

    @NotBlank(message = "报表类型不能为空")
    @Schema(description = "报表类型: ADMISSION, BED, SERVICE_ORDER, CARE_TASK, HEALTH_ALERT, FINANCE_BILLING, IOT_DEVICE, RISK_EVENT")
    private String reportType;

    @Schema(description = "开始日期 (YYYY-MM-DD)")
    private String startDate;

    @Schema(description = "结束日期 (YYYY-MM-DD)")
    private String endDate;

    @Schema(description = "业务状态筛选")
    private String status;

    @Schema(description = "综合搜索关键字")
    private String keyword;

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
