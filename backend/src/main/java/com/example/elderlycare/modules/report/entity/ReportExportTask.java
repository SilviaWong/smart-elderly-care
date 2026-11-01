package com.example.elderlycare.modules.report.entity;

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
 * 异步报表导出任务实体 (report_export_task)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("report_export_task")
public class ReportExportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String taskNo;

    private String reportType;

    private String filterJson;

    private String status;

    private String fileUrl;

    private String fileName;

    private Long totalRows;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
