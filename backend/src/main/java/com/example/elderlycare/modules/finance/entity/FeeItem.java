package com.example.elderlycare.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 费用项目实体 (fee_item)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fee_item")
public class FeeItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String feeCode;

    private String feeName;

    private String feeCategory;

    private String billingUnit;

    private BigDecimal defaultPrice;

    private String status;
}
