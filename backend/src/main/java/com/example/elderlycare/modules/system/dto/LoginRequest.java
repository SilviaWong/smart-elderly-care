package com.example.elderlycare.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录请求参数")
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "登录账号", example = "admin")
    private String username;

    @NotBlank(message = "登录密码不能为空")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @Schema(description = "所属机构ID（可选）", example = "1")
    private Long orgId;
}
