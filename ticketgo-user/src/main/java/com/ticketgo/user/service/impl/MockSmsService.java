package com.ticketgo.user.service.impl;

import com.ticketgo.user.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock 短信：不接真实通道，验证码打印到日志（演示从日志获取）。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ticketgo.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsService implements SmsService {

    @Override
    public void send(String phone, String code) {
        log.info("【Mock短信】手机号={} 验证码={}（生产环境请接入真实短信通道）", phone, code);
    }
}
