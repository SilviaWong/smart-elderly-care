package com.example.elderlycare.modules.risk.vo;

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
@Schema(description = "风险事件视图对象")
public class RiskEventVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "事件流水编号")
    private String eventNo;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者编号")
    private String elderNo;

    @Schema(description = "长者床位信息 (楼栋-房间-床位)")
    private String bedInfo;

    @Schema(description = "事件类型代码")
    private String eventType;

    @Schema(description = "事件类型名称")
    private String eventTypeName;

    @Schema(description = "严重程度代码: LOW, MEDIUM, HIGH, CRITICAL")
    private String severity;

    @Schema(description = "严重程度名称")
    private String severityName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "发生地点")
    private String locationDesc;

    @Schema(description = "事件详细描述")
    private String description;

    @Schema(description = "上报人用户ID")
    private Long reporterId;

    @Schema(description = "上报人真实姓名")
    private String reporterName;

    @Schema(description = "处理负责人用户ID")
    private Long assigneeId;

    @Schema(description = "处理负责人真实姓名")
    private String assigneeName;

    @Schema(description = "事件状态代码: REPORTED, ASSIGNED, PROCESSING, PENDING_REVIEW, CLOSED")
    private String status;

    @Schema(description = "事件状态名称")
    private String statusName;

    @Schema(description = "结案处理结论")
    private String conclusion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "关闭结案时间")
    private LocalDateTime closedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "登记时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否属于高风险或危急级别（需要主管复核）")
    private Boolean requiresReview;
}
