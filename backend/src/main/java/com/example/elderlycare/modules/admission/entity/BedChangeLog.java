package com.example.elderlycare.modules.admission.entity;

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
@TableName("bed_change_log")
public class BedChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long admissionId;

    private Long elderId;

    private Long fromBedId;

    private Long toBedId;

    private String changeType;

    private String changeReason;

    private Long changedBy;

    private LocalDateTime changedAt;
}
