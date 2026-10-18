package com.ticketgo.product.dto;

/**
 * 图片上传返回：三规格 URL
 */
public record UploadResponse(String originalUrl, String displayUrl, String thumbUrl) {
}
