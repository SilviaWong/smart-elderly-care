package com.example.elderlycare.modules.elder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "联系人请求参数")
public class ContactDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "联系人姓名不能为空")
    @Schema(description = "联系人姓名", example = "李大强")
    private String contactName;

    @Schema(description = "与老人关系（如 SON/DAUGHTER/SPOUSE/RELATIVE/OTHER）", example = "SON")
    private String relationship;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话", example = "13911112222")
    private String phone;

    @Schema(description = "是否主要联系人（1是，0否）", example = "1")
    private Integer isPrimary;

    @Schema(description = "是否紧急联系人（1是，0否）", example = "1")
    private Integer isEmergency;

    @Schema(description = "联系地址")
    private String address;
}
