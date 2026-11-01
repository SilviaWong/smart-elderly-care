package com.example.elderlycare.modules.elder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人列表展示模型")
public class ElderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "老人ID")
    private Long id;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "老人档案编号")
    private String elderNo;

    @Schema(description = "老人姓名")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "计算年龄")
    private Integer age;

    @Schema(description = "联系电话（脱敏）")
    private String phoneMasked;

    @Schema(description = "证件号码（脱敏）")
    private String idNoMasked;

    @Schema(description = "主要联系人")
    private String primaryContactName;

    @Schema(description = "主要联系人电话（脱敏）")
    private String primaryContactPhone;

    @Schema(description = "自理能力等级")
    private String mobilityLevel;

    @Schema(description = "档案状态")
    private String status;

    @Schema(description = "建档时间")
    private LocalDateTime createdAt;
}
