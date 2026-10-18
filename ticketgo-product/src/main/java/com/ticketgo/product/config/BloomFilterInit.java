package com.ticketgo.product.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.bloom.RedisBloomFilter;
import com.ticketgo.product.entity.Product;
import com.ticketgo.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时预热布隆过滤器（把所有演出 id 写入），防缓存穿透。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInit implements ApplicationRunner {

    private final RedisBloomFilter bloomFilter;
    private final ProductMapper productMapper;

    private static final String BLOOM_PRODUCT = "bloom:product";

    @Override
    public void run(ApplicationArguments args) {
        bloomFilter.reset(BLOOM_PRODUCT);
        List<Product> products = productMapper.selectList(Wrappers.<Product>lambdaQuery().select(Product::getId));
        for (Product p : products) {
            bloomFilter.add(BLOOM_PRODUCT, String.valueOf(p.getId()));
        }
        log.info("布隆过滤器预热完成，共加载 {} 个演出 id", products.size());
    }
}
