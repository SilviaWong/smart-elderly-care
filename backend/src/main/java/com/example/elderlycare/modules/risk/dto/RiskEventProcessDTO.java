package com.example.elderlycare.modules.risk.dto;

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
@Schema(description = "风险事件处置登记/进展记录参数")
public class RiskEventProcessDTO {

    @NotBlank(message = "处置内容或进展记录不能为空")
    @Schema(description = "处置过程记录/现场措施")
    private String content;

    @Schema(description = "操作类型: START_PROCESS(开始处置), PROGRESS_UPDATE(更新进展)")
    private String action;
}
