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
@Schema(description = "监管模板分页查询参数")
public class RegulatoryTemplateQueryDTO {

    @Schema(description = "模板代码")
    private String templateCode;

    @Schema(description = "模板名称或关键字")
    private String keyword;

    @Schema(description = "业务域: ELDER, BED, CARE, RISK")
    private String businessDomain;

    @Schema(description = "状态: ACTIVE, INACTIVE")
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
