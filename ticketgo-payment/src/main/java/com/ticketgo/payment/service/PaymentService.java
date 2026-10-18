package com.ticketgo.payment.service;

import com.ticketgo.payment.dto.PayCallbackRequest;
import com.ticketgo.payment.dto.PayRequest;
import com.ticketgo.payment.entity.Payment;

public interface PaymentService {

    Payment pay(PayRequest req);

    void callback(PayCallbackRequest req);
}
