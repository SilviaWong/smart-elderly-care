package com.example.elderlycare.modules.iot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备遥测数据视图对象")
public class DeviceDataVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "当前关联就养长者ID")
    private Long elderId;

    @Schema(description = "当前关联长者姓名")
    private String elderName;

    @Schema(description = "指标编码")
    private String metricCode;

    @Schema(description = "指标中文名称")
    private String metricName;

    @Schema(description = "指标数值")
    private BigDecimal metricValue;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "原始报文载荷")
    private String rawPayload;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "设备端采集时间")
    private LocalDateTime collectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "平台端接收存储时间")
    private LocalDateTime receivedAt;
}
