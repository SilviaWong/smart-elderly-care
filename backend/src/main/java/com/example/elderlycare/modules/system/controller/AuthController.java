package com.example.elderlycare.modules.system.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.system.dto.LoginRequest;
import com.example.elderlycare.modules.system.service.SysUserService;
import com.example.elderlycare.modules.system.vo.LoginResponse;
import com.example.elderlycare.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证中心", description = "登录鉴权、Token 签发与个人信息查询")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户密码登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = sysUserService.login(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取当前登录人信息")
    @GetMapping("/me")
    public ApiResponse<UserVO> getCurrentUser() {
        UserVO userVO = sysUserService.getCurrentUser();
        return ApiResponse.success(userVO);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出登录成功", null);
    }
}
