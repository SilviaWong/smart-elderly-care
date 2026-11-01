package com.example.elderlycare.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "权限资源树节点")
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父权限ID")
    private Long parentId;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限类型（MENU/BUTTON）")
    private String permissionType;

    @Schema(description = "前端路由或接口路径")
    private String path;

    @Schema(description = "子权限列表")
    @Builder.Default
    private List<PermissionVO> children = new ArrayList<>();
}
