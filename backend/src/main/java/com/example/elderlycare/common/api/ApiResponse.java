package com.example.elderlycare.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一 API 响应格式")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码（0 代表成功）", example = "0")
    private int code;

    @Schema(description = "响应提示信息", example = "操作成功")
    private String message;

    @Schema(description = "业务响应数据")
    private T data;

    @Schema(description = "响应时间戳（毫秒）", example = "1730467200000")
    private long timestamp;

    @Schema(description = "请求跟踪 ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private String traceId;

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(ErrorCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .traceId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .traceId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(Instant.now().toEpochMilli())
                .traceId(UUID.randomUUID().toString())
                .build();
    }
}
