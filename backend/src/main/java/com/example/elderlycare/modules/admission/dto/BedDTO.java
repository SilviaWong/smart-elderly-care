package com.example.elderlycare.modules.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedDTO {

    @NotNull(message = "所属房间不能为空")
    private Long roomId;

    @NotBlank(message = "床位编号不能为空")
    private String bedNo;

    private String bedType;

    private String status;
}
