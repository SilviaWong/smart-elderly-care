package com.example.elderlycare.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "创建系统角色请求")
public class RoleCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "CAREGIVER")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", example = "一线护理员")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "关联权限ID列表")
    private List<Long> permissionIds;
}
