package com.example.elderlycare.modules.care.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskActionDTO {

    /**
     * 流转动作：START (开始执行), COMPLETE (完成执行), MISS (标记漏执行), CANCEL (取消任务)
     */
    @NotBlank(message = "任务流转动作不能为空（START/COMPLETE/MISS/CANCEL）")
    private String action;

    /**
     * 执行结果代码：NORMAL (正常完成), ABNORMAL (异常反馈), REFUSED (长者拒绝)
     */
    @Builder.Default
    private String resultCode = "NORMAL";

    /**
     * 执行结果详述或异常原因
     */
    private String resultDesc;

    /**
     * 附件照片或检查表单 JSON（可选）
     */
    private String attachmentJson;
}
