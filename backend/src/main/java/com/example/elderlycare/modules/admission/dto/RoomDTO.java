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
public class RoomDTO {

    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    private String floorNo;

    @NotBlank(message = "房间号不能为空")
    private String roomNo;

    private String roomType;

    private String status;
}
