package com.ticketgo.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求：password 与 code 二选一
 */
public record LoginRequest(
        @NotBlank(message = "手机号不能为空") String phone,
        String password,
        String code
) {
}
