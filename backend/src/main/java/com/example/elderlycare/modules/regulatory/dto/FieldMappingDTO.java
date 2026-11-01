package com.example.elderlycare.modules.regulatory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "监管报送模板字段映射配置对象")
public class FieldMappingDTO {

    @Schema(description = "目标监管部门")
    private String targetAuthority;

    @Schema(description = "适用业务域: ELDER, BED, CARE, RISK")
    private String businessDomain;

    @Schema(description = "导出格式: CSV, EXCEL")
    private String fileFormat;

    @Schema(description = "报送规范说明")
    private String description;

    @Schema(description = "字段映射明细列表")
    private List<FieldItem> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldItem {
        @NotBlank(message = "源属性名不能为空")
        private String sourceField;

        @NotBlank(message = "监管目标字段名不能为空")
        private String targetField;

        private String fieldType; // STRING, INTEGER, DECIMAL, DATE, DATETIME
        private Boolean required; // 是否必填
        private String validationRule; // NOT_BLANK, ID_CARD, PHONE, REGEX:..., NUMBER_RANGE:...
        private String transformRule; // 枚举对照 MALE:1-男,FEMALE:2-女
        private String defaultValue;
        private Integer sortOrder;
    }
}
