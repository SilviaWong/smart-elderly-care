package com.example.elderlycare.modules.regulatory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成监管报送批次请求参数")
public class RegulatoryBatchGenerateDTO {

    @NotNull(message = "报送模板ID不能为空")
    @Schema(description = "报送模板ID")
    private Long templateId;

    @Schema(description = "数据统计周期开始日期 (YYYY-MM-DD)")
    private LocalDate periodStart;

    @Schema(description = "数据统计周期结束日期 (YYYY-MM-DD)")
    private LocalDate periodEnd;

    @Schema(description = "批次备注说明")
    private String remark;
}
