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
@Schema(description = "风险事件提交复核参数")
public class RiskEventSubmitReviewDTO {

    @NotBlank(message = "复核申请说明与处置总结不能为空")
    @Schema(description = "处置情况总结及申请复核说明")
    private String content;
}
