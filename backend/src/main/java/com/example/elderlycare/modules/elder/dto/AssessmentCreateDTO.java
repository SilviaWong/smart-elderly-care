package com.example.elderlycare.modules.elder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人能力评估创建参数")
public class AssessmentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "评估类型不能为空")
    @Schema(description = "评估类型（如 ADL、COGNITIVE、FALL_RISK 等）", example = "ADL")
    private String assessmentType;

    @NotNull(message = "评估日期不能为空")
    @Schema(description = "评估执行日期", example = "2026-11-01")
    private LocalDate assessmentDate;

    @Schema(description = "综合得分", example = "85.50")
    private BigDecimal score;

    @Schema(description = "评估结论等级（如 自理、轻度失能、中度失能、重度失能）", example = "轻度失能")
    private String resultLevel;

    @Schema(description = "详细评估问卷题目及得分（JSON 字符串）")
    private String detailJson;

    @Schema(description = "评估备注说明")
    private String remark;
}
