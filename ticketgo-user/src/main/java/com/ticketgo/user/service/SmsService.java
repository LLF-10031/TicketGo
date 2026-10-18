package com.ticketgo.user.service;

/**
 * 短信通道抽象：开发用 Mock，生产替换真实 SDK，业务代码无感。
 */
public interface SmsService {
    void send(String phone, String code);
}
