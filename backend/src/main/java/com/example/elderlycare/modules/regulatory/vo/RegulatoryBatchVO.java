package com.example.elderlycare.modules.regulatory.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "监管报送批次视图对象")
public class RegulatoryBatchVO {

    @Schema(description = "批次ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "报送批次单号")
    private String batchNo;

    @Schema(description = "报送模板ID")
    private Long templateId;

    @Schema(description = "模板代码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "业务域")
    private String businessDomain;

    @Schema(description = "目标监管机构")
    private String targetAuthority;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "数据统计开始日期")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "数据统计结束日期")
    private LocalDate periodEnd;

    @Schema(description = "批次状态: GENERATED, VALIDATED, SUBMITTED, REJECTED, CONFIRMED")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;

    @Schema(description = "合规报送文件下载URL")
    private String fileUrl;

    @Schema(description = "合规报送文件名")
    private String fileName;

    @Schema(description = "前置数据校验结果汇总")
    private BatchValidationResultVO validationResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "正式提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "监管反馈结果与回执单号")
    private String responseDesc;

    @Schema(description = "批次创建人ID")
    private Long createdBy;

    @Schema(description = "批次创建人姓名")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "批次生成时间")
    private LocalDateTime createdAt;
}
