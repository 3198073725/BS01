package com.vidsprout.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean ok;
    private String message;
    private T data;
    private T results;
    private Integer count;
    private Integer page;
    private Integer pageSize;
    private Integer total;
    private Boolean hasNext;
    private String next;
    private String previous;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .ok(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .ok(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> successWithResults(T results, int count) {
        return ApiResponse.<T>builder()
                .ok(true)
                .results(results)
                .count(count)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .ok(false)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .ok(true)
                .message("创建成功")
                .data(data)
                .build();
    }
}
