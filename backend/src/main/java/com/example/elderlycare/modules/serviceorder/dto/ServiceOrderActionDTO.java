package com.example.elderlycare.modules.serviceorder.dto;

import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderActionDTO {

    @NotNull(message = "流转操作动作不能为空")
    private ServiceOrderAction action;

    /**
     * 执行结果（当 action 为 COMPLETE 时建议填写）
     */
    private String executionResult;

    /**
     * 备注、拒单原因或退回原因
     */
    private String remark;
}
