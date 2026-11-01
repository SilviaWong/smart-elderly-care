package com.example.elderlycare.modules.iot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备遥测采集数据实体 (device_data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_data")
public class DeviceData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long deviceId;

    private String metricCode;

    private BigDecimal metricValue;

    private String unit;

    private String rawPayload;

    private LocalDateTime collectedAt;

    private LocalDateTime receivedAt;
}
