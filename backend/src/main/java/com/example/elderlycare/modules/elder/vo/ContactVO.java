package com.example.elderlycare.modules.elder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "联系人展示模型")
public class ContactVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "联系人ID")
    private Long id;

    @Schema(description = "老人ID")
    private Long elderId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "与老人关系")
    private String relationship;

    @Schema(description = "联系电话（脱敏）")
    private String phoneMasked;

    @Schema(description = "完整联系电话（仅限授权查看）")
    private String phone;

    @Schema(description = "是否主要联系人（1是，0否）")
    private Integer isPrimary;

    @Schema(description = "是否紧急联系人（1是，0否）")
    private Integer isEmergency;

    @Schema(description = "联系地址")
    private String address;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
