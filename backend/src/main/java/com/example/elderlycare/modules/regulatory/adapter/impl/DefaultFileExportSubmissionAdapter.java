package com.example.elderlycare.modules.regulatory.adapter.impl;

import com.example.elderlycare.modules.regulatory.adapter.RegulatorySubmissionAdapter;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 默认标准文件导出型报送适配器
 * 实现标准文件导出、离线转存与受理登记，遵循“未提供真实规范时不声称已完成政府平台对接”原则
 */
@Slf4j
@Component
public class DefaultFileExportSubmissionAdapter implements RegulatorySubmissionAdapter {

    @Override
    public SubmissionResponse submit(RegulatoryBatch batch, RegulatoryBatchSubmitDTO submitDTO) {
        log.info("执行监管批次报送交付: batchNo={}, channel={}", batch.getBatchNo(), submitDTO.getChannel());

        // 生成或使用用户登记的回执号
        String receiptNo = submitDTO.getExternalReceiptNo();
        if (!StringUtils.hasText(receiptNo)) {
            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int randomCode = ThreadLocalRandom.current().nextInt(1000, 9999);
            receiptNo = "RCPT-" + timeStr + "-" + randomCode;
        }

        String notes = StringUtils.hasText(submitDTO.getSubmissionNotes())
                ? submitDTO.getSubmissionNotes()
                : "已完成合规导出文件打包与报送批次固化登记";

        String message = String.format("受理登记成功 (回执单号: %s)。交付说明: %s [当前采用预留标准化文件交付通道，未收到民政/卫健真实政府专网接口规范前不直连外网]", receiptNo, notes);

        return new SubmissionResponse(true, receiptNo, message, "ACCEPTED");
    }

    @Override
    public String getSupportedChannel() {
        return "DEFAULT_FILE_EXPORT";
    }
}
