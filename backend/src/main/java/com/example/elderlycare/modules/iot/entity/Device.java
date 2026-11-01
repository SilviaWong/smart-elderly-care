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
 * 物联网设备台账实体 (device)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String vendor;

    private String model;

    private String serialNo;

    private String installLocation;

    private String status;

    private LocalDateTime lastOnlineAt;

    private LocalDateTime createdAt;
}
