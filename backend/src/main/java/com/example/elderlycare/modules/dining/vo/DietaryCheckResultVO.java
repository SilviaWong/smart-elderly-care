package com.example.elderlycare.modules.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "过敏原与饮食禁忌风险筛查结果")
public class DietaryCheckResultVO {

    @Schema(description = "是否触发过敏原或禁忌预警")
    private Boolean hasWarning;

    @Schema(description = "预警警示信息列表")
    private List<String> warnings;

    @Schema(description = "长者膳食信息")
    private ElderDietaryProfileVO elderProfile;
}
