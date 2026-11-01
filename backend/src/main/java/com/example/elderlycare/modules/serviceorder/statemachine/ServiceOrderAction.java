package com.example.elderlycare.modules.serviceorder.statemachine;

import lombok.Getter;

@Getter
public enum ServiceOrderAction {

    CREATE("创建工单"),
    ASSIGN("派发工单"),
    ACCEPT("接单确认"),
    START("开始执行"),
    COMPLETE("完成执行"),
    CONFIRM("验收通过"),
    REJECT("拒绝接单"),
    RETURN("退回工单"),
    CANCEL("取消工单");

    private final String description;

    ServiceOrderAction(String description) {
        this.description = description;
    }
}
