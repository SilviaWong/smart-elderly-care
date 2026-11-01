package com.example.elderlycare.modules.elder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "老人档案列表分页查询条件")
public class ElderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private long pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private long pageSize = 10;

    @Schema(description = "老人档案编号")
    private String elderNo;

    @Schema(description = "老人姓名")
    private String name;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "证件号码（精确查询）")
    private String idNo;

    @Schema(description = "老人状态（PENDING/IN_HOUSE/DISCHARGED/ARCHIVED）")
    private String status;

    @Schema(description = "自理能力等级")
    private String mobilityLevel;
}
