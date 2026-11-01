package com.example.elderlycare.modules.system.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.system.dto.UserCreateDTO;
import com.example.elderlycare.modules.system.dto.UserQueryDTO;
import com.example.elderlycare.modules.system.dto.UserUpdateDTO;
import com.example.elderlycare.modules.system.service.SysUserService;
import com.example.elderlycare.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统用户管理", description = "用户分页、新增、编辑与启停")
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询机构用户列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:user:view')")
    public ApiResponse<PageResult<UserVO>> pageUsers(UserQueryDTO query) {
        PageResult<UserVO> pageResult = sysUserService.pageUsers(query);
        return ApiResponse.success(pageResult);
    }

    @Operation(summary = "创建机构新用户")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:user:edit')")
    public ApiResponse<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        Long userId = sysUserService.createUser(dto);
        return ApiResponse.success(userId);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:user:edit')")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        sysUserService.updateUser(id, dto);
        return ApiResponse.success("更新用户成功", null);
    }

    @Operation(summary = "修改用户状态（ACTIVE/DISABLED）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN') or hasAuthority('system:user:edit')")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        sysUserService.changeUserStatus(id, status);
        return ApiResponse.success("更新状态成功", null);
    }
}
