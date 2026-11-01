package com.example.elderlycare.modules.report.vo;

import com.example.elderlycare.common.api.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用业务报表查询结果")
public class BusinessReportResultVO {

    @Schema(description = "报表类型代码")
    private String reportType;

    @Schema(description = "报表类型中文名")
    private String reportTypeName;

    @Schema(description = "表头列定义列表")
    private List<ReportColumnDef> columns;

    @Schema(description = "分页数据行")
    private PageResult<Map<String, Object>> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportColumnDef {
        private String prop;
        private String label;
        private Integer width;
        private String align; // left, center, right
    }
}
