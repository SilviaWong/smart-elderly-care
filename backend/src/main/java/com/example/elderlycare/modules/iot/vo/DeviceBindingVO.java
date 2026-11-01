package com.example.elderlycare.modules.iot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备绑定历史记录视图对象")
public class DeviceBindingVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者编号")
    private String elderNo;

    @Schema(description = "床位ID")
    private Long bedId;

    @Schema(description = "床位信息")
    private String bedInfo;

    @Schema(description = "绑定类型: ELDER, BED, SPACE")
    private String bindType;

    @Schema(description = "绑定类型中文名")
    private String bindTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "绑定时间")
    private LocalDateTime boundAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "解绑时间")
    private LocalDateTime unboundAt;

    @Schema(description = "绑定状态: BOUND, UNBOUND")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;
}
