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
@Schema(description = "月度账单分页查询参数")
public class BillingQueryDTO {

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "账单月份 (yyyy-MM)")
    private String billMonth;

    @Schema(description = "账单状态: DRAFT, UNPAID, PARTIAL, PAID, OVERDUE, CANCELLED")
    private String status;

    @Schema(description = "关键词 (账单号/长者姓名/备注)")
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
