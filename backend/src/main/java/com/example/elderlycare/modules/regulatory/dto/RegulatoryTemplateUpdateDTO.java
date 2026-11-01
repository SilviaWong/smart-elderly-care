package com.example.elderlycare.modules.regulatory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "修改监管报送模板请求参数")
public class RegulatoryTemplateUpdateDTO {

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "版本号")
    private String versionNo;

    @Valid
    @Schema(description = "字段映射详细配置")
    private FieldMappingDTO fieldMapping;

    @Schema(description = "启用状态: ACTIVE, INACTIVE")
    private String status;
}
