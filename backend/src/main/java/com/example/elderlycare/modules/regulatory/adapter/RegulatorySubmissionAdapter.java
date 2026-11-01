package com.example.elderlycare.modules.regulatory.adapter;

import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;

/**
 * 预留外部监管平台接口适配器契约 (Adapter Pattern)
 * 遵循监管报送解耦规范：预留外部接口扩展点，未经真实政务接口规范与授权前，不声称已对接官方专网
 */
public interface RegulatorySubmissionAdapter {

    /**
     * 提交报送批次
     *
     * @param batch 报送批次实体
     * @param submitDTO 提交参数
     * @return 监管受理结果与回执
     */
    SubmissionResponse submit(RegulatoryBatch batch, RegulatoryBatchSubmitDTO submitDTO);

    /**
     * 该适配器支持的报送渠道代码
     */
    String getSupportedChannel();

    /**
     * 报送响应结构
     */
    record SubmissionResponse(
            boolean success,
            String receiptNo,
            String responseMessage,
            String externalStatus
    ) {}
}
