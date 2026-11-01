package com.example.elderlycare.modules.system.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.system.dto.RoleCreateDTO;
import com.example.elderlycare.modules.system.dto.RoleUpdateDTO;
import com.example.elderlycare.modules.system.service.SysRoleService;
import com.example.elderlycare.modules.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统角色管理", description = "角色列表、新增、编辑、删除与权限指派")
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "查询当前机构角色列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:role:view')")
    public ApiResponse<List<RoleVO>> listRoles() {
        List<RoleVO> list = sysRoleService.listRoles();
        return ApiResponse.success(list);
    }

    @Operation(summary = "创建机构新角色")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:role:edit')")
    public ApiResponse<Long> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        Long roleId = sysRoleService.createRole(dto);
        return ApiResponse.success(roleId);
    }

    @Operation(summary = "编辑角色信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:role:edit')")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        sysRoleService.updateRole(id, dto);
        return ApiResponse.success("更新角色成功", null);
    }

    @Operation(summary = "获取角色已分配的权限ID列表")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:role:view')")
    public ApiResponse<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = sysRoleService.getRolePermissionIds(id);
        return ApiResponse.success(permissionIds);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:role:edit')")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return ApiResponse.success("删除角色成功", null);
    }
}
