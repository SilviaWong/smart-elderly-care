package com.example.elderlycare.modules.serviceorder.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.serviceorder.dto.ServiceItemDTO;
import com.example.elderlycare.modules.serviceorder.service.ServiceItemService;
import com.example.elderlycare.modules.serviceorder.vo.ServiceItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "服务项目管理", description = "机构照护、康复、保洁等服务项目目录配置接口")
@RestController
@RequestMapping("/api/v1/service-items")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @Operation(summary = "查询服务项目列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<List<ServiceItemVO>> listItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(serviceItemService.listItems(category, status));
    }

    @Operation(summary = "查询单个服务项目详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<ServiceItemVO> getItemById(@PathVariable Long id) {
        return ApiResponse.success(serviceItemService.getItemById(id));
    }

    @Operation(summary = "新增服务项目")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Long> createItem(@Valid @RequestBody ServiceItemDTO dto) {
        return ApiResponse.success(serviceItemService.createItem(dto));
    }

    @Operation(summary = "更新服务项目")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @Valid @RequestBody ServiceItemDTO dto) {
        serviceItemService.updateItem(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/停用服务项目")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Void> toggleItemStatus(@PathVariable Long id, @RequestParam String status) {
        serviceItemService.toggleItemStatus(id, status);
        return ApiResponse.success();
    }
}
