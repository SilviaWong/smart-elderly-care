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
@Schema(description = "主管复核审批参数")
public class RiskEventReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "是否通过: true-审核通过并关闭结案, false-驳回重新处置")
    private Boolean approved;

    @Schema(description = "审核结论（通过结案时必填）")
    private String conclusion;

    @Schema(description = "驳回原因与指导意见（驳回时必填）")
    private String rejectReason;
}
