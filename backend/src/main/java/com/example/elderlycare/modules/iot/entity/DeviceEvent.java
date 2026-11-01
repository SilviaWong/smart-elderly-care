package com.example.elderlycare.modules.iot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物联网设备事件实体 (device_event)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_event")
public class DeviceEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long deviceId;

    private String eventType;

    private String severity;

    private String eventPayload;

    private String eventStatus;

    private LocalDateTime occurredAt;

    private Long handledBy;

    private LocalDateTime handledAt;

    private String handleResult;

    private LocalDateTime createdAt;
}
