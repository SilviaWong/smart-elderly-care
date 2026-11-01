package com.example.elderlycare.modules.care.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private CarePlanVO plan;

    private List<CarePlanItemVO> items;
}
