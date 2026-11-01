package com.example.elderlycare.modules.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "费用项目视图对象")
public class FeeItemVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "费用编码")
    private String feeCode;

    @Schema(description = "费用名称")
    private String feeName;

    @Schema(description = "费用分类代码")
    private String feeCategory;

    @Schema(description = "费用分类中文名")
    private String feeCategoryName;

    @Schema(description = "计费单位")
    private String billingUnit;

    @Schema(description = "基准单价")
    private BigDecimal defaultPrice;

    @Schema(description = "状态代码: ACTIVE, INACTIVE")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;
}
