package com.example.elderlycare.modules.system.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.system.service.SysPermissionService;
import com.example.elderlycare.modules.system.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统权限资源", description = "权限树结构查询")
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    @Operation(summary = "获取系统全量权限树")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PermissionVO>> getPermissionTree() {
        List<PermissionVO> tree = sysPermissionService.getPermissionTree();
        return ApiResponse.success(tree);
    }
}
