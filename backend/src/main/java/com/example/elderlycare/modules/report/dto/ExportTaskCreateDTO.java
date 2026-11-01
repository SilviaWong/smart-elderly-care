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
@Schema(description = "创建异步报表导出任务参数")
public class ExportTaskCreateDTO {

    @NotBlank(message = "报表类型不能为空")
    @Schema(description = "报表类型: ADMISSION(入住退住), BED(床位使用), SERVICE_ORDER(工单服务), CARE_TASK(护理任务), HEALTH_ALERT(健康预警), FINANCE_BILLING(财务账单), IOT_DEVICE(物联设备), RISK_EVENT(安全风险)")
    private String reportType;

    @Schema(description = "导出过滤参数 JSON 字符串")
    private String filterJson;

    @Schema(description = "自定义导出文件名 (选填)")
    private String customFileName;
}
