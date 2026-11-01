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
@Schema(description = "风险事件结案关闭参数")
public class RiskEventCloseDTO {

    @NotBlank(message = "处理结论不能为空")
    @Schema(description = "结案处理结论、调查结果与善后防范措施")
    private String conclusion;
}
