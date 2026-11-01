package com.example.elderlycare.modules.regulatory.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateCreateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateUpdateDTO;
import com.example.elderlycare.modules.regulatory.service.RegulatoryTemplateService;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "监管报送 - 报送模板与字段映射管理", description = "民政、卫健、应急等部门监管报送模板维护、规则配置与字段映射接口")
@RestController
@RequestMapping("/api/v1/regulatory/templates")
@RequiredArgsConstructor
public class RegulatoryTemplateController {

    private final RegulatoryTemplateService templateService;

    @Operation(summary = "创建监管报送模板")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('regulatory')")
    public ApiResponse<Long> createTemplate(@Valid @RequestBody RegulatoryTemplateCreateDTO dto) {
        return ApiResponse.success(templateService.createTemplate(dto));
    }

    @Operation(summary = "修改监管报送模板及字段映射")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('regulatory')")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody RegulatoryTemplateUpdateDTO dto) {
        templateService.updateTemplate(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "删除监管报送模板")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN') or hasAuthority('regulatory')")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取指定监管模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<RegulatoryTemplateVO> getTemplateDetail(@PathVariable Long id) {
        return ApiResponse.success(templateService.getTemplateDetail(id));
    }

    @Operation(summary = "分页查询监管报送模板列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<PageResult<RegulatoryTemplateVO>> pageTemplates(RegulatoryTemplateQueryDTO query) {
        return ApiResponse.success(templateService.pageTemplates(query));
    }

    @Operation(summary = "查询所有启用的有效模板列表 (下拉选框用)")
    @GetMapping("/active-list")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<List<RegulatoryTemplateVO>> listActiveTemplates() {
        return ApiResponse.success(templateService.listActiveTemplates());
    }
}
