package com.example.elderlycare.modules.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "账单视图对象")
public class BillingVO {

    @Schema(description = "账单ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "账单编号")
    private String billNo;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者编号")
    private String elderNo;

    @Schema(description = "长者床位位置 (楼栋-房间-床位)")
    private String bedInfo;

    @Schema(description = "入住办理记录ID")
    private Long admissionId;

    @Schema(description = "账单月份 (yyyy-MM)")
    private String billMonth;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "出账日期")
    private LocalDate billDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "应缴款截止日期")
    private LocalDate dueDate;

    @Schema(description = "账单应收总金额 (明细合计)")
    private BigDecimal totalAmount;

    @Schema(description = "账单已收金额 (有效收款 - 成功退款)")
    private BigDecimal paidAmount;

    @Schema(description = "待缴清欠费金额 (应收 - 已收)")
    private BigDecimal unpaidAmount;

    @Schema(description = "账单状态代码: DRAFT, UNPAID, PARTIAL, PAID, OVERDUE, CANCELLED")
    private String status;

    @Schema(description = "账单状态中文名称")
    private String statusName;

    @Schema(description = "账单备注")
    private String remark;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建人真实姓名")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否已逾期")
    private Boolean isOverdue;

    @Schema(description = "收费项目明细列表")
    private List<BillingItemVO> items;
}
