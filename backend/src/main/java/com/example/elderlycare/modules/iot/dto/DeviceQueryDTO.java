package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备列表查询参数")
public class DeviceQueryDTO {

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备状态: ACTIVE, INACTIVE, OFFLINE, FAULT")
    private String status;

    @Schema(description = "绑定状态: BOUND(已绑定), UNBOUND(空闲未绑定)")
    private String bindStatus;

    @Schema(description = "关键词 (设备编码/名称/SN号/位置)")
    private String keyword;

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
