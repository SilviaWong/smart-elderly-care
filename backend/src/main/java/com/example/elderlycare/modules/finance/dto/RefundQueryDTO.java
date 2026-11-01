package com.example.elderlycare.modules.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款记录查询参数")
public class RefundQueryDTO {

    @Schema(description = "原收款记录ID")
    private Long paymentId;

    @Schema(description = "退款状态: PENDING, APPROVED, SUCCESS, REJECTED")
    private String status;

    @Schema(description = "关键词 (退款单号/原因)")
    private String keyword;

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
