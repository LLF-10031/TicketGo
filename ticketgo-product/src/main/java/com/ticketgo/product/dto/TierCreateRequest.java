package com.ticketgo.product.dto;

import java.math.BigDecimal;

/**
 * 管理端创建票档请求
 */
public record TierCreateRequest(String name, BigDecimal price, Integer totalStock) {
}
