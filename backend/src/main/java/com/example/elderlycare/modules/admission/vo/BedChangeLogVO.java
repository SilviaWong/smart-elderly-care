package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedChangeLogVO {

    private Long id;

    private Long admissionId;

    private Long elderId;

    private String elderName;

    private Long fromBedId;

    private String fromBedNo;

    private Long toBedId;

    private String toBedNo;

    private String changeType;

    private String changeReason;

    private String changedByName;

    private LocalDateTime changedAt;
}
