package com.example.elderlycare.modules.risk.vo;

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
@Schema(description = "风险事件详情视图对象（包含基本信息与处理轨迹时间线）")
public class RiskEventDetailVO {

    @Schema(description = "风险事件主信息")
    private RiskEventVO event;

    @Schema(description = "全流程处理与流转日志轨迹时间线")
    private List<RiskEventLogVO> logs;
}
