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
@Schema(description = "监管报送批次分页查询参数")
public class RegulatoryBatchQueryDTO {

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "批次单号")
    private String batchNo;

    @Schema(description = "批次状态: GENERATED, VALIDATED, SUBMITTED, REJECTED, CONFIRMED")
    private String status;

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
