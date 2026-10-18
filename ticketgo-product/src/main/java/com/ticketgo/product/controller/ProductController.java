package com.ticketgo.product.controller;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.Result;
import com.ticketgo.product.entity.Product;
import com.ticketgo.product.entity.Session;
import com.ticketgo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 演出浏览（C 端）
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/product/list")
    public Result<PageResult<Product>> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(productService.list(category, status, keyword, page, pageSize));
    }

    @GetMapping("/api/product/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @GetMapping("/api/product/{id}/sessions")
    public Result<List<Session>> sessions(@PathVariable Long id) {
        return Result.ok(productService.sessions(id));
    }

    @GetMapping("/api/session/{id}/tiers")
    public Result<List<com.ticketgo.product.entity.TicketTier>> tiers(@PathVariable Long id) {
        return Result.ok(productService.tiers(id));
    }
}
