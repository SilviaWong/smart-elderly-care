package com.example.elderlycare.modules.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导出任务分页查询参数")
public class ExportTaskQueryDTO {

    @Schema(description = "报表类型")
    private String reportType;

    @Schema(description = "任务状态: PENDING(排队中), PROCESSING(处理中), SUCCESS(导出成功), FAILED(失败)")
    private String status;

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
