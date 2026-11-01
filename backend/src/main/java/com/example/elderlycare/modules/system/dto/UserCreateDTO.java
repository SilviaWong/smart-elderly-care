package com.example.elderlycare.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "创建系统用户请求")
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 个字符之间")
    @Schema(description = "登录账号", example = "caregiver2")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度不少于 6 位")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "真实姓名", example = "李护工")
    private String realName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "关联角色ID列表")
    private List<Long> roleIds;
}
