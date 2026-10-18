package com.ticketgo.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "手机号不能为空") String phone,
        String password,
        String nickName,
        String code
) {
}
