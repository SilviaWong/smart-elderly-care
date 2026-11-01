package com.example.elderlycare.modules.regulatory.entity;

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
 * 监管报送模板实体 (regulatory_template)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("regulatory_template")
public class RegulatoryTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String templateCode;

    private String templateName;

    private String versionNo;

    private String fieldMappingJson;

    private String status; // ACTIVE, INACTIVE

    private LocalDateTime createdAt;
}
