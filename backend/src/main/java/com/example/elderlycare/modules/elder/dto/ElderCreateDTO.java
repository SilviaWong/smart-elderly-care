package com.example.elderlycare.modules.elder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "老人建档请求参数")
public class ElderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "老人档案编号不能为空")
    @Schema(description = "机构内唯一档案编号", example = "ELD-202611-001")
    private String elderNo;

    @NotBlank(message = "老人姓名不能为空")
    @Schema(description = "老人姓名", example = "张建国")
    private String name;

    @Schema(description = "性别（MALE、FEMALE、OTHER）", example = "MALE")
    private String gender;

    @Schema(description = "出生日期", example = "1948-05-12")
    private LocalDate birthDate;

    @Schema(description = "证件类型（ID_CARD、PASSPORT 等）", example = "ID_CARD")
    private String idType;

    @Schema(description = "证件号码（入库将自动加密存储并建立哈希索引）", example = "110101194805121234")
    private String idNo;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "家庭常住地址")
    private String address;

    @Schema(description = "照片 URL")
    private String photoUrl;

    @Schema(description = "血型（A/B/AB/O/OTHER）", example = "A")
    private String bloodType;

    @Schema(description = "过敏史描述")
    private String allergyDesc;

    @Schema(description = "慢病史描述")
    private String chronicDiseaseDesc;

    @Schema(description = "日常用药说明")
    private String medicationDesc;

    @Schema(description = "饮食禁忌说明")
    private String dietTaboo;

    @Schema(description = "自理能力等级（SELF_CARE、PARTIAL、ASSISTED、BEDRIDDEN）", example = "PARTIAL")
    private String mobilityLevel;

    @Schema(description = "档案备注")
    private String remark;

    @Schema(description = "初始联系人列表")
    private List<ContactDTO> contacts;
}
