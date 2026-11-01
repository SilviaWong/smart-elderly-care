package com.example.elderlycare.modules.elder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人详细全景档案视图模型")
public class ElderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "老人ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "老人编号")
    private String elderNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "计算年龄")
    private Integer age;

    @Schema(description = "证件类型")
    private String idType;

    @Schema(description = "证件号码（脱敏展示）")
    private String idNoMasked;

    @Schema(description = "联系电话（脱敏展示）")
    private String phoneMasked;

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

    @Schema(description = "常用药物")
    private String medicationDesc;

    @Schema(description = "饮食禁忌")
    private String dietTaboo;

    @Schema(description = "自理能力")
    private String mobilityLevel;

    @Schema(description = "当前档案状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "建档人姓名")
    private String creatorName;

    @Schema(description = "建档时间")
    private LocalDateTime createdAt;

    @Schema(description = "联系人列表")
    private List<ContactVO> contacts;

    @Schema(description = "历史评估记录列表")
    private List<AssessmentVO> assessments;
}
