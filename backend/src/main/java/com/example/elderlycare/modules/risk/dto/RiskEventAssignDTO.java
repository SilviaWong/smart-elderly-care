package com.example.elderlycare.modules.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "风险事件分派负责人参数")
public class RiskEventAssignDTO {

    @NotNull(message = "负责人用户ID不能为空")
    @Schema(description = "指定处理负责人ID")
    private Long assigneeId;

    @Schema(description = "分派交待与备注要求")
    private String remark;
}
