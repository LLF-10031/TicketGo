package com.ticketgo.user.dto;

public record TokenResponse(String accessToken, String refreshToken, Long userId, String nickName, String icon) {
}
