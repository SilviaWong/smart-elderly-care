package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionPrecheckVO {

    private Boolean canDischarge;

    private List<String> warnings;

    private List<String> blockingReasons;
}
