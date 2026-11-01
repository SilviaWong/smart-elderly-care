package com.example.elderlycare.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应数据")
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT 认证令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "当前机构ID")
    private Long orgId;

    @Schema(description = "当前机构名称")
    private String orgName;

    @Schema(description = "所属角色列表")
    private List<String> roles;

    @Schema(description = "拥有权限编码列表")
    private List<String> permissions;
}
