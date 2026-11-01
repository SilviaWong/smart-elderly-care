package com.example.elderlycare.modules.dining.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.MealMenuCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealMenuQueryDTO;
import com.example.elderlycare.modules.dining.service.MealMenuService;
import com.example.elderlycare.modules.dining.vo.MealMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "餐饮管理 - 餐次菜单维护", description = "每日早中晚餐次菜单编排、发布与明细接口")
@RestController
@RequestMapping("/api/v1/dining/menus")
@RequiredArgsConstructor
public class MealMenuController {

    private final MealMenuService mealMenuService;

    @Operation(summary = "分页查询餐次菜单")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<PageResult<MealMenuVO>> pageMenus(MealMenuQueryDTO query) {
        return ApiResponse.success(mealMenuService.pageMenus(query));
    }

    @Operation(summary = "按日期和餐次查询菜单及菜品列表")
    @GetMapping("/by-date-type")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<MealMenuVO> getMenuByDateAndType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate menuDate,
            @RequestParam String mealType) {
        return ApiResponse.success(mealMenuService.getMenuByDateAndType(menuDate, mealType));
    }

    @Operation(summary = "查询菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<MealMenuVO> getMenuDetail(@PathVariable Long id) {
        return ApiResponse.success(mealMenuService.getMenuDetail(id));
    }

    @Operation(summary = "保存或更新每日餐次菜单（自动级联生成菜品价格快照）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('dining')")
    public ApiResponse<Long> saveOrUpdateMenu(@Valid @RequestBody MealMenuCreateDTO dto) {
        return ApiResponse.success(mealMenuService.saveOrUpdateMenu(dto));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('dining')")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        mealMenuService.deleteMenu(id);
        return ApiResponse.success();
    }
}
