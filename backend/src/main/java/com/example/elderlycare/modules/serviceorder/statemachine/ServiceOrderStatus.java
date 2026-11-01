package com.example.elderlycare.modules.serviceorder.statemachine;

import lombok.Getter;

@Getter
public enum ServiceOrderStatus {

    PENDING_ASSIGN("待派单"),
    ASSIGNED("已派单"),
    ACCEPTED("已接单"),
    IN_PROGRESS("执行中"),
    PENDING_CONFIRM("待验收"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已拒绝"),
    RETURNED("已退回");

    private final String description;

    ServiceOrderStatus(String description) {
        this.description = description;
    }
}
