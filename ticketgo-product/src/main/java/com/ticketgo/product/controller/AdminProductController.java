package com.ticketgo.product.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.product.dto.ProductCreateRequest;
import com.ticketgo.product.dto.SessionCreateRequest;
import com.ticketgo.product.dto.TierCreateRequest;
import com.ticketgo.product.entity.Product;
import com.ticketgo.product.entity.Session;
import com.ticketgo.product.entity.TicketTier;
import com.ticketgo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：演出 / 场次 / 票档
 */
@RestController
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping("/api/admin/product")
    public Result<Product> createProduct(@RequestBody ProductCreateRequest req) {
        return Result.ok(productService.createProduct(req));
    }

    @PostMapping("/api/admin/product/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        productService.publish(id);
        return Result.ok();
    }

    @PostMapping("/api/admin/session")
    public Result<Session> createSession(@RequestBody SessionCreateRequest req) {
        return Result.ok(productService.createSession(req));
    }

    @PostMapping("/api/admin/session/{id}/tier")
    public Result<TicketTier> createTier(@PathVariable Long id, @RequestBody TierCreateRequest req) {
        return Result.ok(productService.createTier(id, req));
    }

    @PutMapping("/api/admin/tier/{id}/stock")
    public Result<Void> adjustStock(@PathVariable Long id, @RequestParam Integer stock) {
        productService.adjustStock(id, stock);
        return Result.ok();
    }
}
