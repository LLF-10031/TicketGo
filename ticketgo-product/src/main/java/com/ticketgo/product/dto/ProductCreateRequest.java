package com.ticketgo.product.dto;

/**
 * 管理端创建演出请求
 */
public record ProductCreateRequest(String name, String category, String description, String poster) {
}
