package com.example.elderlycare.modules.iot.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceEventHandleDTO;
import com.example.elderlycare.modules.iot.dto.DeviceEventQueryDTO;
import com.example.elderlycare.modules.iot.service.DeviceEventService;
import com.example.elderlycare.modules.iot.vo.DeviceEventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "物联网管理 - 设备事件与告警", description = "设备离线、低电量、异常读数、SOS、跌倒等事件处置与跟踪接口")
@RestController
@RequestMapping("/api/v1/iot/events")
@RequiredArgsConstructor
public class DeviceEventController {

    private final DeviceEventService deviceEventService;

    @Operation(summary = "分页查询设备事件列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<PageResult<DeviceEventVO>> pageEvents(DeviceEventQueryDTO query) {
        return ApiResponse.success(deviceEventService.pageEvents(query));
    }

    @Operation(summary = "获取设备事件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<DeviceEventVO> getEventDetail(@PathVariable Long id) {
        return ApiResponse.success(deviceEventService.getEventDetail(id));
    }

    @Operation(summary = "处理设备告警事件")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<Void> handleEvent(@PathVariable Long id, @Valid @RequestBody DeviceEventHandleDTO dto) {
        deviceEventService.handleEvent(id, dto);
        return ApiResponse.success();
    }
}
