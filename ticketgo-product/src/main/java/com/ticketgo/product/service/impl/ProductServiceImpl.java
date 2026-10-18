package com.ticketgo.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketgo.common.bloom.RedisBloomFilter;
import com.ticketgo.common.cache.CacheClient;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.product.dto.ProductCreateRequest;
import com.ticketgo.product.dto.SessionCreateRequest;
import com.ticketgo.product.dto.TierCreateRequest;
import com.ticketgo.product.entity.Product;
import com.ticketgo.product.entity.Session;
import com.ticketgo.product.entity.StockAudit;
import com.ticketgo.product.entity.TicketTier;
import com.ticketgo.product.mapper.ProductMapper;
import com.ticketgo.product.mapper.SessionMapper;
import com.ticketgo.product.mapper.StockAuditMapper;
import com.ticketgo.product.mapper.TicketTierMapper;
import com.ticketgo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final SessionMapper sessionMapper;
    private final TicketTierMapper ticketTierMapper;
    private final StockAuditMapper stockAuditMapper;
    private final CacheClient cacheClient;
    private final RedisBloomFilter bloomFilter;

    /** 异步重建缓存的线程池（防击穿） */
    private final ExecutorService rebuildExecutor = Executors.newFixedThreadPool(4);

    private static final String BLOOM_PRODUCT = "bloom:product";
    private static final long PRODUCT_CACHE_SECONDS = 300;

    @Override
    public PageResult<Product> list(String category, Integer status, String keyword, int page, int pageSize) {
        Page<Product> p = new Page<>(Math.max(page, 1), pageSize <= 0 ? 20 : pageSize);
        var wrapper = Wrappers.<Product>lambdaQuery();
        if (StringUtils.hasText(category)) {
            wrapper.eq(Product::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        productMapper.selectPage(p, wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    @Override
    public Product detail(Long id) {
        // 防穿透：布隆过滤器先拦
        if (!bloomFilter.contains(BLOOM_PRODUCT, String.valueOf(id))) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 防击穿：逻辑过期 + 互斥锁 + 异步重建；防雪崩：随机 TTL
        Product product = cacheClient.getWithLogicalExpire(
                RedisKeys.CACHE_PRODUCT, id, Product.class, PRODUCT_CACHE_SECONDS,
                key -> productMapper.selectById((Long) key), rebuildExecutor);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return product;
    }

    @Override
    public List<Session> sessions(Long productId) {
        return sessionMapper.selectList(Wrappers.<Session>lambdaQuery()
                .eq(Session::getProductId, productId)
                .orderByAsc(Session::getShowTime));
    }

    @Override
    public List<TicketTier> tiers(Long sessionId) {
        return ticketTierMapper.selectList(Wrappers.<TicketTier>lambdaQuery()
                .eq(TicketTier::getSessionId, sessionId)
                .orderByAsc(TicketTier::getPrice));
    }

    @Override
    public Product createProduct(ProductCreateRequest req) {
        if (req == null || !StringUtils.hasText(req.name())) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Product product = new Product();
        product.setId(SnowflakeIdGenerator.generateId());
        product.setName(req.name());
        product.setCategory(req.category());
        product.setDescription(req.description());
        product.setPoster(req.poster());
        product.setStatus(0); // 草稿
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        // 同步布隆过滤器
        bloomFilter.add(BLOOM_PRODUCT, String.valueOf(product.getId()));
        return product;
    }

    @Override
    public void publish(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        product.setStatus(1); // 已上架
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        // 更新后删除缓存（下次读重建）
        cacheClient.delete(RedisKeys.CACHE_PRODUCT + id);
    }

    @Override
    public Session createSession(SessionCreateRequest req) {
        if (req == null || req.productId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (productMapper.selectById(req.productId()) == null) {
            throw new BizException("演出不存在");
        }
        Session session = new Session();
        session.setId(SnowflakeIdGenerator.generateId());
        session.setProductId(req.productId());
        session.setVenue(req.venue());
        session.setShowTime(req.showTime());
        session.setSaleStartTime(req.saleStartTime());
        session.setSaleEndTime(req.saleEndTime());
        session.setSeatMode(req.seatMode() == null ? 0 : req.seatMode());
        session.setStatus(0); // 待开票
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public TicketTier createTier(Long sessionId, TierCreateRequest req) {
        if (req == null || !StringUtils.hasText(req.name()) || req.price() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (sessionMapper.selectById(sessionId) == null) {
            throw new BizException("场次不存在");
        }
        TicketTier tier = new TicketTier();
        tier.setId(SnowflakeIdGenerator.generateId());
        tier.setSessionId(sessionId);
        tier.setName(req.name());
        tier.setPrice(req.price());
        tier.setTotalStock(req.totalStock() == null ? 0 : req.totalStock());
        tier.setSoldStock(0);
        tier.setCreateTime(LocalDateTime.now());
        tier.setUpdateTime(LocalDateTime.now());
        ticketTierMapper.insert(tier);
        return tier;
    }

    @Override
    public void adjustStock(Long tierId, Integer totalStock) {
        TicketTier tier = ticketTierMapper.selectById(tierId);
        if (tier == null) {
            throw new BizException("票档不存在");
        }
        int before = tier.getTotalStock() == null ? 0 : tier.getTotalStock();
        int after = totalStock == null ? before : totalStock;
        tier.setTotalStock(after);
        tier.setUpdateTime(LocalDateTime.now());
        ticketTierMapper.updateById(tier);
        // 库存审计
        StockAudit audit = new StockAudit();
        audit.setId(SnowflakeIdGenerator.generateId());
        audit.setTargetType("tier");
        audit.setTargetId(tierId);
        audit.setBeforeQty(before);
        audit.setAfterQty(after);
        audit.setOperator("admin");
        audit.setRemark("管理端调整票档库存");
        audit.setCreateTime(LocalDateTime.now());
        stockAuditMapper.insert(audit);
        log.info("票档库存调整 tierId={} {} -> {}（已审计）", tierId, before, after);
    }
}
