package com.example.elderlycare.modules.serviceorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_order")
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String orderNo;

    private Long elderId;

    private Long admissionId;

    private Long serviceItemId;

    private String sourceType;

    private String priority;

    private LocalDateTime expectedStartAt;

    private Long assigneeId;

    private Long creatorId;

    private String status;

    private String requestDesc;

    private String executionResult;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
