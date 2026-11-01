package com.example.elderlycare.modules.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款记录视图对象")
public class RefundRecordVO {

    @Schema(description = "退款记录ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "退款流水编号")
    private String refundNo;

    @Schema(description = "关联原收款记录ID")
    private Long paymentId;

    @Schema(description = "原收款流水号")
    private String paymentNo;

    @Schema(description = "关联账单ID")
    private Long billingId;

    @Schema(description = "账单单号")
    private String billNo;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "退款金额")
    private BigDecimal amount;

    @Schema(description = "退款具体原因")
    private String reason;

    @Schema(description = "退款状态代码: PENDING(待审核), APPROVED(已审批), SUCCESS(退款成功), REJECTED(驳回)")
    private String status;

    @Schema(description = "退款状态中文名称")
    private String statusName;

    @Schema(description = "审批人ID")
    private Long approvedBy;

    @Schema(description = "审批人真实姓名")
    private String approvedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际退款到账时间")
    private LocalDateTime refundedAt;

    @Schema(description = "申请人ID")
    private Long createdBy;

    @Schema(description = "申请人姓名")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请创建时间")
    private LocalDateTime createdAt;
}
