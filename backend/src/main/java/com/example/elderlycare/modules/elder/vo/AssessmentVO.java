package com.example.elderlycare.modules.elder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人评估展示模型")
public class AssessmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评估记录ID")
    private Long id;

    @Schema(description = "老人ID")
    private Long elderId;

    @Schema(description = "评估类型")
    private String assessmentType;

    @Schema(description = "评估日期")
    private LocalDate assessmentDate;

    @Schema(description = "评估得分")
    private BigDecimal score;

    @Schema(description = "评定等级")
    private String resultLevel;

    @Schema(description = "题目明细 JSON")
    private String detailJson;

    @Schema(description = "评估人员ID")
    private Long assessorId;

    @Schema(description = "评估人员姓名")
    private String assessorName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
