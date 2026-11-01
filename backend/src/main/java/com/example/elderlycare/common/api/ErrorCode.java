package com.example.elderlycare.common.api;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录凭证已失效"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "所请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法"),
    CONFLICT(409, "数据冲突或已存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部繁忙，请稍后再试"),

    // 业务通用错误码 (1000 - 1999)
    PARAM_VALIDATION_ERROR(1001, "参数校验失败"),
    ORG_ACCESS_DENIED(1002, "无权访问此机构数据"),
    RESOURCE_NOT_FOUND(1003, "指定业务记录不存在"),
    IDEMPOTENCY_CONFLICT(1004, "重复请求或相同业务已在处理中"),
    BUSINESS_ERROR(1005, "业务规则处理失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
