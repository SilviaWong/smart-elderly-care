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
 * 设备绑定历史记录实体 (device_binding)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_binding")
public class DeviceBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long deviceId;

    private Long elderId;

    private Long bedId;

    private String bindType;

    private LocalDateTime boundAt;

    private LocalDateTime unboundAt;

    private String status;
}
