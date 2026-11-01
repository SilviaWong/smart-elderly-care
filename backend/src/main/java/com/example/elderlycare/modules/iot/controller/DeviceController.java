package com.example.elderlycare.modules.iot.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceBindDTO;
import com.example.elderlycare.modules.iot.dto.DeviceDTO;
import com.example.elderlycare.modules.iot.dto.DeviceQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceUnbindDTO;
import com.example.elderlycare.modules.iot.service.DeviceService;
import com.example.elderlycare.modules.iot.vo.DeviceBindingVO;
import com.example.elderlycare.modules.iot.vo.DeviceVO;
import com.example.elderlycare.modules.iot.vo.IotSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "物联网管理 - 设备台账与绑定", description = "设备档案、长者/床位绑定与解绑、绑定历史及统计看板接口")
@RestController
@RequestMapping("/api/v1/iot/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "分页查询设备列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<PageResult<DeviceVO>> pageDevices(DeviceQueryDTO query) {
        return ApiResponse.success(deviceService.pageDevices(query));
    }

    @Operation(summary = "获取可用设备列表")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<List<DeviceVO>> listActiveDevices(@RequestParam(required = false) String deviceType) {
        return ApiResponse.success(deviceService.listActiveDevices(deviceType));
    }

    @Operation(summary = "获取设备详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<DeviceVO> getDeviceDetail(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDeviceDetail(id));
    }

    @Operation(summary = "新增设备台账")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('iot')")
    public ApiResponse<Long> createDevice(@Valid @RequestBody DeviceDTO dto) {
        return ApiResponse.success(deviceService.createDevice(dto));
    }

    @Operation(summary = "修改设备信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('iot')")
    public ApiResponse<Void> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceDTO dto) {
        deviceService.updateDevice(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "设备绑定 (绑定长者/床位/空间)")
    @PostMapping("/bind")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<Void> bindDevice(@Valid @RequestBody DeviceBindDTO dto) {
        deviceService.bindDevice(dto);
        return ApiResponse.success();
    }

    @Operation(summary = "设备解绑")
    @PostMapping("/unbind")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<Void> unbindDevice(@Valid @RequestBody DeviceUnbindDTO dto) {
        deviceService.unbindDevice(dto);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询设备绑定历史记录")
    @GetMapping("/bindings")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<PageResult<DeviceBindingVO>> pageBindings(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long elderId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(deviceService.pageBindings(deviceId, elderId, pageNum, pageSize));
    }

    @Operation(summary = "物联网综合看板统计指标")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('iot')")
    public ApiResponse<IotSummaryVO> getIotSummary() {
        return ApiResponse.success(deviceService.getIotSummary());
    }
}
