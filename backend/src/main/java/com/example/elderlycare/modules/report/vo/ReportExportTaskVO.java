package com.example.elderlycare.modules.report.vo;

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
@Schema(description = "异步报表导出任务视图对象")
public class ReportExportTaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "导出任务单号")
    private String taskNo;

    @Schema(description = "报表类型代码")
    private String reportType;

    @Schema(description = "报表类型中文名")
    private String reportTypeName;

    @Schema(description = "过滤参数 JSON")
    private String filterJson;

    @Schema(description = "任务状态: PENDING, PROCESSING, SUCCESS, FAILED")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;

    @Schema(description = "下载URL路径")
    private String fileUrl;

    @Schema(description = "导出的物理文件名")
    private String fileName;

    @Schema(description = "导出记录总行数")
    private Long totalRows;

    @Schema(description = "失败原因说明")
    private String errorMessage;

    @Schema(description = "发起人ID")
    private Long createdBy;

    @Schema(description = "发起人姓名")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始处理时间")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "文件过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "文件是否已过期不可下载")
    private Boolean isExpired;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "任务创建时间")
    private LocalDateTime createdAt;
}
