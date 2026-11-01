package com.example.elderlycare.modules.finance.dto;

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
@Schema(description = "退款审批参数")
public class RefundAuditDTO {

    @NotNull(message = "审批结果不能为空")
    @Schema(description = "审批结果: true-审批通过并执行退款入账, false-驳回退款")
    private Boolean approved;

    @Schema(description = "审批处理意见或驳回原因说明")
    private String remark;
}
