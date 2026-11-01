package com.example.elderlycare.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一分页响应结构")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private long pageNum;

    @Schema(description = "每页大小", example = "10")
    private long pageSize;

    @Schema(description = "总页数", example = "10")
    private long totalPages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        long totalPages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        return PageResult.<T>builder()
                .list(list != null ? list : Collections.emptyList())
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(Collections.emptyList(), 0L, pageNum, pageSize);
    }
}
