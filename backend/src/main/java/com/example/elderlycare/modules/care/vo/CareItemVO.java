package com.example.elderlycare.modules.care.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private String itemCode;

    private String itemName;

    private String category;

    private String description;

    private String status;
}
