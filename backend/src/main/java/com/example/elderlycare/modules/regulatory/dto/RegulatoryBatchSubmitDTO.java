package com.example.elderlycare.modules.regulatory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登记报送提交参数")
public class RegulatoryBatchSubmitDTO {

    @Schema(description = "报送渠道: DEFAULT_FILE_EXPORT(标准文件报送), MOCK_GOV_ADAPTER(政务系统模拟直连), OFFLINE_DISK(离线专网拷贝)")
    private String channel;

    @Schema(description = "外部监管回执单号/受理编号 (非必填，若有可手动登记)")
    private String externalReceiptNo;

    @Schema(description = "报送提交备注与说明")
    private String submissionNotes;
}
