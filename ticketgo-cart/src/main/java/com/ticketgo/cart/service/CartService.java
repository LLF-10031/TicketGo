package com.ticketgo.cart.service;

import com.ticketgo.cart.dto.CartAddRequest;
import com.ticketgo.cart.dto.CartCheckoutRequest;
import com.ticketgo.cart.entity.Cart;

import java.util.List;

public interface CartService {

    List<Cart> list(Long userId);

    Cart add(Long userId, CartAddRequest req);

    void remove(Long userId, Long id);

    Long checkout(Long userId, CartCheckoutRequest req);
}
