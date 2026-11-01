package com.example.elderlycare.modules.care.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.care.dto.CareItemDTO;
import com.example.elderlycare.modules.care.dto.CareLevelDTO;
import com.example.elderlycare.modules.care.service.CareConfigService;
import com.example.elderlycare.modules.care.vo.CareItemVO;
import com.example.elderlycare.modules.care.vo.CareLevelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "护理等级与护理项目配置", description = "养老机构护理分级体系及标准化护理项目字典维护接口")
@RestController
@RequestMapping("/api/v1/care/config")
@RequiredArgsConstructor
public class CareConfigController {

    private final CareConfigService careConfigService;

    // --- 护理等级 ---
    @Operation(summary = "查询护理等级列表")
    @GetMapping("/levels")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<List<CareLevelVO>> listLevels(@RequestParam(required = false) String status) {
        return ApiResponse.success(careConfigService.listLevels(status));
    }

    @Operation(summary = "新增护理等级")
    @PostMapping("/levels")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Long> createLevel(@Valid @RequestBody CareLevelDTO dto) {
        return ApiResponse.success(careConfigService.createLevel(dto));
    }

    @Operation(summary = "修改护理等级")
    @PutMapping("/levels/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody CareLevelDTO dto) {
        careConfigService.updateLevel(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启停护理等级")
    @PutMapping("/levels/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> toggleLevelStatus(@PathVariable Long id, @RequestParam String status) {
        careConfigService.toggleLevelStatus(id, status);
        return ApiResponse.success();
    }

    // --- 护理项目 ---
    @Operation(summary = "查询护理项目列表")
    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<List<CareItemVO>> listItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(careConfigService.listItems(category, status));
    }

    @Operation(summary = "新增护理项目")
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Long> createItem(@Valid @RequestBody CareItemDTO dto) {
        return ApiResponse.success(careConfigService.createItem(dto));
    }

    @Operation(summary = "修改护理项目")
    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @Valid @RequestBody CareItemDTO dto) {
        careConfigService.updateItem(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启停护理项目")
    @PutMapping("/items/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> toggleItemStatus(@PathVariable Long id, @RequestParam String status) {
        careConfigService.toggleItemStatus(id, status);
        return ApiResponse.success();
    }
}
