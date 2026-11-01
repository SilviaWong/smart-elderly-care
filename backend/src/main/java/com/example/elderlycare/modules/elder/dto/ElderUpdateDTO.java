package com.example.elderlycare.modules.elder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人档案修改请求参数")
public class ElderUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "老人姓名不能为空")
    @Schema(description = "老人姓名", example = "张建国")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "证件类型")
    private String idType;

    @Schema(description = "修改证件号码（留空表示不修改）")
    private String idNo;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "家庭常住地址")
    private String address;

    @Schema(description = "照片 URL")
    private String photoUrl;

    @Schema(description = "血型")
    private String bloodType;

    @Schema(description = "过敏史")
    private String allergyDesc;

    @Schema(description = "慢病史")
    private String chronicDiseaseDesc;

    @Schema(description = "用药说明")
    private String medicationDesc;

    @Schema(description = "饮食禁忌")
    private String dietTaboo;

    @Schema(description = "自理能力等级")
    private String mobilityLevel;

    @Schema(description = "老人档案状态（PENDING/IN_HOUSE/DISCHARGED/ARCHIVED）")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
