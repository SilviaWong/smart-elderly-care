package com.example.elderlycare.modules.regulatory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "监管报送数据前置校验结果视图")
public class BatchValidationResultVO {

    @Schema(description = "抽取数据总行数")
    private Integer totalRows;

    @Schema(description = "校验合格行数")
    private Integer passedRows;

    @Schema(description = "包含异常错误的行数")
    private Integer failedRows;

    @Schema(description = "综合校验状态: ALL_PASSED(全部通过), CONTAINS_ERRORS(存在错误), EMPTY_DATA(无数据)")
    private String validationStatus;

    @Schema(description = "校验执行时间")
    private String validatedAt;

    @Schema(description = "错误明细列表 (精确定位到行号、业务记录ID、字段与原因)")
    private List<ValidationErrorItemVO> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorItemVO {
        @Schema(description = "数据行号 (1-indexed)")
        private Integer rowNum;

        @Schema(description = "源业务主键ID")
        private Long recordId;

        @Schema(description = "源实体属性名")
        private String fieldName;

        @Schema(description = "监管字段展示名称")
        private String fieldLabel;

        @Schema(description = "被校验驳回的原值")
        private Object rejectedValue;

        @Schema(description = "错误类型: MISSING_REQUIRED(必填缺失), FORMAT_INVALID(格式错误), RULE_VIOLATION(规则不符), BIZ_CONFLICT(业务冲突)")
        private String errorType;

        @Schema(description = "详细错误说明")
        private String errorMessage;
    }
}
