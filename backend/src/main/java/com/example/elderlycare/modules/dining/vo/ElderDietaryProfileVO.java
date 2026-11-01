package com.example.elderlycare.modules.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "长者膳食健康档案视图对象")
public class ElderDietaryProfileVO {

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者编号")
    private String elderNo;

    @Schema(description = "药物/食物过敏史")
    private String allergyDesc;

    @Schema(description = "膳食与饮食禁忌")
    private String dietTaboo;

    @Schema(description = "慢病健康情况")
    private String chronicDiseaseDesc;
}
