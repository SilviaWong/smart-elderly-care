package com.example.elderlycare.modules.regulatory.vo;

import com.example.elderlycare.modules.regulatory.dto.FieldMappingDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "监管报送模板视图对象")
public class RegulatoryTemplateVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "模板代码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "字段映射详细配置")
    private FieldMappingDTO fieldMapping;

    @Schema(description = "映射字段总数")
    private Integer fieldCount;

    @Schema(description = "模板状态: ACTIVE, INACTIVE")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
