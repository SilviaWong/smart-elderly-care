package com.example.elderlycare.modules.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("care_plan_item")
public class CarePlanItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long planId;

    private Long careItemId;

    private String frequencyType;

    private String frequencyValue;

    private LocalTime timeWindowStart;

    private LocalTime timeWindowEnd;

    private Long assigneeId;

    private Integer enabled;

    private LocalDateTime createdAt;
}
