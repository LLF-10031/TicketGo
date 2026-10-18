package com.ticketgo.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应体：{ total, items }
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private List<T> items;

    public PageResult() {
    }

    public PageResult(long total, List<T> items) {
        this.total = total;
        this.items = items;
    }

    public static <T> PageResult<T> of(long total, List<T> items) {
        return new PageResult<>(total, items);
    }
}
