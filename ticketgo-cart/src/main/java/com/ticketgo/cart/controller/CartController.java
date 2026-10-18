package com.ticketgo.cart.controller;

import com.ticketgo.cart.dto.CartAddRequest;
import com.ticketgo.cart.dto.CartCheckoutRequest;
import com.ticketgo.cart.entity.Cart;
import com.ticketgo.cart.service.CartService;
import com.ticketgo.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/api/cart")
    public Result<List<Cart>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(cartService.list(userId));
    }

    @PostMapping("/api/cart/add")
    public Result<Cart> add(@RequestHeader("X-User-Id") Long userId, @RequestBody CartAddRequest req) {
        return Result.ok(cartService.add(userId, req));
    }

    @DeleteMapping("/api/cart/{id}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        cartService.remove(userId, id);
        return Result.ok();
    }

    @PostMapping("/api/cart/checkout")
    public Result<Long> checkout(@RequestHeader("X-User-Id") Long userId, @RequestBody CartCheckoutRequest req) {
        return Result.ok(cartService.checkout(userId, req));
    }
}
