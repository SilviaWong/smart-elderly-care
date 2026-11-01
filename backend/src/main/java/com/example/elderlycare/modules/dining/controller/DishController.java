package com.example.elderlycare.modules.dining.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.DishDTO;
import com.example.elderlycare.modules.dining.dto.DishQueryDTO;
import com.example.elderlycare.modules.dining.service.DishService;
import com.example.elderlycare.modules.dining.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "餐饮管理 - 菜品维护", description = "菜品增删改查、在售状态与过敏原管理接口")
@RestController
@RequestMapping("/api/v1/dining/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @Operation(summary = "分页查询菜品列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<PageResult<DishVO>> pageDishes(DishQueryDTO query) {
        return ApiResponse.success(dishService.pageDishes(query));
    }

    @Operation(summary = "获取所有在售菜品列表（用于配置菜单和快捷点餐）")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<List<DishVO>> listActiveDishes() {
        return ApiResponse.success(dishService.listActiveDishes());
    }

    @Operation(summary = "获取菜品详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<DishVO> getDish(@PathVariable Long id) {
        return ApiResponse.success(dishService.getDish(id));
    }

    @Operation(summary = "创建菜品")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('dining')")
    public ApiResponse<Long> createDish(@Valid @RequestBody DishDTO dto) {
        return ApiResponse.success(dishService.createDish(dto));
    }

    @Operation(summary = "更新菜品")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('dining')")
    public ApiResponse<Void> updateDish(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        dishService.updateDish(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "切换菜品售卖状态 (ACTIVE / INACTIVE)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('dining')")
    public ApiResponse<Void> toggleDishStatus(@PathVariable Long id, @RequestParam String status) {
        dishService.toggleDishStatus(id, status);
        return ApiResponse.success();
    }
}
