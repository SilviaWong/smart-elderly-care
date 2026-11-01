package com.example.elderlycare.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "系统用户分页查询条件")
public class UserQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private long pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private long pageSize = 10;

    @Schema(description = "用户账号关键字")
    private String username;

    @Schema(description = "真实姓名关键字")
    private String realName;

    @Schema(description = "状态过滤", example = "ACTIVE")
    private String status;
}
