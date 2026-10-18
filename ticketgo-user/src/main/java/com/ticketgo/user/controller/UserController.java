package com.ticketgo.user.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.user.dto.CaptchaResponse;
import com.ticketgo.user.dto.LoginRequest;
import com.ticketgo.user.dto.RegisterRequest;
import com.ticketgo.user.dto.TokenResponse;
import com.ticketgo.user.entity.User;
import com.ticketgo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ---------- 验证码 ----------

    @GetMapping("/api/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.ok(userService.getCaptcha());
    }

    @PostMapping("/api/sms/send")
    public Result<Void> sendSms(@RequestBody SmsSendRequest req) {
        userService.sendSms(req.phone(), req.captchaId(), req.captchaInput());
        return Result.ok();
    }

    // ---------- 注册/登录 ----------

    @PostMapping("/api/user/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req));
    }

    @PostMapping("/api/user/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    @PostMapping("/api/user/refresh")
    public Result<TokenResponse> refresh(@RequestHeader("refreshToken") String refreshToken) {
        return Result.ok(userService.refresh(refreshToken));
    }

    @PostMapping("/api/user/logout")
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String authorization) {
        String token = stripBearer(authorization);
        userService.logout(token);
        return Result.ok();
    }

    // ---------- 个人中心 ----------

    @GetMapping("/api/user/profile")
    public Result<User> profile(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(userService.getProfile(userId));
    }

    @PutMapping("/api/user/profile")
    public Result<Void> updateProfile(@RequestHeader("X-User-Id") Long userId, @RequestBody ProfileUpdateRequest req) {
        userService.updateProfile(userId, req.nickName());
        return Result.ok();
    }

    @PutMapping("/api/user/avatar")
    public Result<Void> updateAvatar(@RequestHeader("X-User-Id") Long userId, @RequestBody AvatarUpdateRequest req) {
        userService.updateAvatar(userId, req.avatarUrl());
        return Result.ok();
    }

    @PutMapping("/api/user/password")
    public Result<Void> updatePassword(@RequestHeader("X-User-Id") Long userId, @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(userId, req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    private String stripBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }

    // ---------- 请求体 ----------
    public record SmsSendRequest(String phone, String captchaId, String captchaInput) {
    }

    public record ProfileUpdateRequest(String nickName) {
    }

    public record AvatarUpdateRequest(String avatarUrl) {
    }

    public record PasswordUpdateRequest(String oldPassword, String newPassword) {
    }
}
