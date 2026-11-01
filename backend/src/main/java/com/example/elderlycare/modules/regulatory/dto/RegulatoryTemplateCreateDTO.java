package com.example.elderlycare.modules.regulatory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建监管报送模板请求参数")
public class RegulatoryTemplateCreateDTO {

    @NotBlank(message = "模板代码不能为空")
    @Schema(description = "模板代码")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称")
    private String templateName;

    @NotBlank(message = "版本号不能为空")
    @Schema(description = "版本号，如 v1.0")
    private String versionNo;

    @NotNull(message = "字段映射配置不能为空")
    @Valid
    @Schema(description = "字段映射详细配置")
    private FieldMappingDTO fieldMapping;

    @Schema(description = "启用状态: ACTIVE, INACTIVE")
    private String status;
}
