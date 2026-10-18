package com.ticketgo.payment.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.payment.dto.PayCallbackRequest;
import com.ticketgo.payment.dto.PayRequest;
import com.ticketgo.payment.entity.Payment;
import com.ticketgo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/payment/pay")
    public Result<Payment> pay(@RequestBody PayRequest req) {
        return Result.ok(paymentService.pay(req));
    }

    @PostMapping("/api/payment/callback")
    public Result<Void> callback(@RequestBody PayCallbackRequest req) {
        paymentService.callback(req);
        return Result.ok();
    }
}
