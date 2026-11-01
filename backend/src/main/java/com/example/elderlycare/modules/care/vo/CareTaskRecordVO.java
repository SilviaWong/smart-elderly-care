package com.example.elderlycare.modules.care.vo;

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
public class CareTaskRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long taskId;

    private Long executorId;

    private String executorName;

    private LocalDateTime executedAt;

    private String resultCode;

    private String resultDesc;

    private String attachmentJson;

    private LocalDateTime createdAt;
}
