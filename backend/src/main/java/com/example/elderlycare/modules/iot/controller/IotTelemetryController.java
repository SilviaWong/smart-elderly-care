package com.example.elderlycare.modules.iot.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceDataQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceReportDTO;
import com.example.elderlycare.modules.iot.service.IotTelemetryService;
import com.example.elderlycare.modules.iot.vo.DeviceDataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "物联网管理 - 模拟设备接入与遥测数据", description = "设备数据上报模拟接收、厂商协议适配与历史遥测数据查询接口")
@RestController
@RequestMapping("/api/v1/iot/telemetry")
@RequiredArgsConstructor
public class IotTelemetryController {

    private final IotTelemetryService iotTelemetryService;

    @Operation(summary = "模拟设备数据上报接收 (支持多厂商适配器解析与异常事件自动识别)")
    @PostMapping("/report")
    public ApiResponse<Void> reportTelemetry(@Valid @RequestBody DeviceReportDTO dto) {
        iotTelemetryService.reportTelemetry(dto);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询设备遥测采集数据历史")
    @GetMapping("/data")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<PageResult<DeviceDataVO>> pageTelemetryData(DeviceDataQueryDTO query) {
        return ApiResponse.success(iotTelemetryService.pageTelemetryData(query));
    }
}
