package com.ticketgo.user.service;

import com.ticketgo.user.dto.CaptchaResponse;
import com.ticketgo.user.dto.LoginRequest;
import com.ticketgo.user.dto.RegisterRequest;
import com.ticketgo.user.dto.TokenResponse;
import com.ticketgo.user.entity.User;

public interface UserService {

    CaptchaResponse getCaptcha();

    void sendSms(String phone, String captchaId, String captchaInput);

    TokenResponse register(RegisterRequest req);

    TokenResponse login(LoginRequest req);

    TokenResponse refresh(String refreshToken);

    void logout(String accessToken);

    User getProfile(Long userId);

    void updateProfile(Long userId, String nickName);

    void updateAvatar(Long userId, String avatarUrl);

    void updatePassword(Long userId, String oldPassword, String newPassword);
}
