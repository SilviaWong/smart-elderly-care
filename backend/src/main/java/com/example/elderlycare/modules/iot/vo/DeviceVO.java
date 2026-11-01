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
@Schema(description = "设备台账视图对象")
public class DeviceVO {

    @Schema(description = "设备主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型代码")
    private String deviceType;

    @Schema(description = "设备类型中文名称")
    private String deviceTypeName;

    @Schema(description = "设备厂商")
    private String vendor;

    @Schema(description = "规格型号")
    private String model;

    @Schema(description = "出厂序列号")
    private String serialNo;

    @Schema(description = "安装/摆放位置")
    private String installLocation;

    @Schema(description = "设备状态代码: ACTIVE, INACTIVE, OFFLINE, FAULT")
    private String status;

    @Schema(description = "设备状态中文名称")
    private String statusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后一次在线/通信时间")
    private LocalDateTime lastOnlineAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    // 当前绑定状态信息
    @Schema(description = "当前绑定状态: BOUND(已绑定), UNBOUND(空闲未绑定)")
    private String bindStatus;

    @Schema(description = "当前绑定类型: ELDER, BED, SPACE")
    private String bindType;

    @Schema(description = "当前绑定长者ID")
    private Long boundElderId;

    @Schema(description = "当前绑定长者姓名")
    private String boundElderName;

    @Schema(description = "当前绑定床位ID")
    private Long boundBedId;

    @Schema(description = "当前绑定床位信息")
    private String boundBedInfo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "绑定发生时间")
    private LocalDateTime boundAt;
}
