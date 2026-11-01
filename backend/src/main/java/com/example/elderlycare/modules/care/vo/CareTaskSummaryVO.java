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
public class CareTaskSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalTasks;

    private Integer pendingCount;

    private Integer inProgressCount;

    private Integer completedCount;

    private Integer missedCount;

    private Integer cancelledCount;
}
