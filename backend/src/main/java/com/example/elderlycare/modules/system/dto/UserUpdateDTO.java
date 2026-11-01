package com.example.elderlycare.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "更新系统用户请求")
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "李护工")
    private String realName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "修改密码（留空表示不修改）")
    private String password;

    @Schema(description = "用户状态", example = "ACTIVE")
    private String status;

    @Schema(description = "关联角色ID列表")
    private List<Long> roleIds;
}
