package com.ticketgo.product.service;

import com.ticketgo.common.result.PageResult;
import com.ticketgo.product.dto.ProductCreateRequest;
import com.ticketgo.product.dto.SessionCreateRequest;
import com.ticketgo.product.dto.TierCreateRequest;
import com.ticketgo.product.entity.Product;
import com.ticketgo.product.entity.Session;
import com.ticketgo.product.entity.TicketTier;

import java.util.List;

public interface ProductService {

    PageResult<Product> list(String category, Integer status, String keyword, int page, int pageSize);

    Product detail(Long id);

    List<Session> sessions(Long productId);

    List<TicketTier> tiers(Long sessionId);

    Product createProduct(ProductCreateRequest req);

    void publish(Long id);

    Session createSession(SessionCreateRequest req);

    TicketTier createTier(Long sessionId, TierCreateRequest req);

    void adjustStock(Long tierId, Integer totalStock);
}
