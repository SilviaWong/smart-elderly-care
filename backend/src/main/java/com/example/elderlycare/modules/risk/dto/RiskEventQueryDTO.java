package com.example.elderlycare.modules.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "风险事件分页与筛选查询参数")
public class RiskEventQueryDTO {

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    @Schema(description = "关键字（编号、地点、描述、结论）")
    private String keyword;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "严重程度: LOW, MEDIUM, HIGH, CRITICAL")
    private String severity;

    @Schema(description = "事件状态: REPORTED, ASSIGNED, PROCESSING, PENDING_REVIEW, CLOSED")
    private String status;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "处理负责人ID")
    private Long assigneeId;

    @Schema(description = "上报人ID")
    private Long reporterId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发生时间起")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发生时间止")
    private LocalDateTime endTime;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
