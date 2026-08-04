package com.vidsprout.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    @Builder.Default
    private int page = 1;
    @Builder.Default
    private int size = 20;
    private String sort;
    private String order;

    public int getOffset() {
        return (page - 1) * size;
    }
}
