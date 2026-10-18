package com.ticketgo.cart.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.cart.dto.CartAddRequest;
import com.ticketgo.cart.dto.CartCheckoutRequest;
import com.ticketgo.cart.entity.Cart;
import com.ticketgo.cart.entity.Order;
import com.ticketgo.cart.entity.OrderItem;
import com.ticketgo.cart.mapper.CartMapper;
import com.ticketgo.cart.mapper.OrderItemMapper;
import com.ticketgo.cart.mapper.OrderMapper;
import com.ticketgo.cart.mapper.TierPriceMapper;
import com.ticketgo.cart.service.CartService;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TierPriceMapper tierPriceMapper;
    private final RestTemplate restTemplate;

    @Value("${ticketgo.coupon.base-url:http://127.0.0.1:8087}")
    private String couponBaseUrl;

    private static final int PAY_EXPIRE_MINUTES = 15;

    @Override
    public List<Cart> list(Long userId) {
        return cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
    }

    @Override
    public Cart add(Long userId, CartAddRequest req) {
        if (req == null || req.sessionId() == null || req.tierId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Cart existing = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSessionId, req.sessionId())
                .eq(Cart::getTierId, req.tierId())
                .isNull(Cart::getSeatId)
                .last("limit 1"));
        int quantity = req.quantity() == null || req.quantity() <= 0 ? 1 : req.quantity();
        if (existing != null && req.seatId() == null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(existing);
            return existing;
        }
        Cart cart = new Cart();
        cart.setId(SnowflakeIdGenerator.generateId());
        cart.setUserId(userId);
        cart.setSessionId(req.sessionId());
        cart.setTierId(req.tierId());
        cart.setSeatId(req.seatId());
        cart.setQuantity(quantity);
        cart.setCreateTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.insert(cart);
        return cart;
    }

    @Override
    public void remove(Long userId, Long id) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        cartMapper.deleteById(id);
    }

    @Override
    public Long checkout(Long userId, CartCheckoutRequest req) {
        if (req == null || CollectionUtils.isEmpty(req.cartItemIds())) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, req.cartItemIds()));
        if (carts.isEmpty()) {
            throw new BizException("购物车为空");
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = BigDecimal.ZERO;
        for (Cart cart : carts) {
            BigDecimal price = tierPriceMapper.selectPrice(cart.getTierId());
            if (price == null) {
                price = BigDecimal.ZERO;
            }
            total = total.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }
        long orderId = SnowflakeIdGenerator.generateId();
        BigDecimal payAmount = total;
        if (req.userCouponId() != null) {
            payAmount = lockCoupon(req.userCouponId(), orderId, userId, total, req.productId());
        }
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo("TG" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6));
        order.setUserId(userId);
        order.setOrderType(1);
        order.setSessionId(carts.get(0).getSessionId());
        order.setTotalAmount(payAmount);
        order.setStatus(0);
        order.setPayExpireAt(now.plusMinutes(PAY_EXPIRE_MINUTES));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        for (Cart cart : carts) {
            BigDecimal price = tierPriceMapper.selectPrice(cart.getTierId());
            OrderItem item = new OrderItem();
            item.setId(SnowflakeIdGenerator.generateId());
            item.setOrderId(order.getId());
            item.setTierId(cart.getTierId());
            item.setSeatId(cart.getSeatId());
            item.setPrice(price == null ? BigDecimal.ZERO : price);
            item.setQuantity(cart.getQuantity());
            item.setCreateTime(now);
            orderItemMapper.insert(item);
            cartMapper.deleteById(cart.getId());
        }
        log.info("购物车结算生成订单 orderNo={} amount={}", order.getOrderNo(), payAmount);
        return order.getId();
    }

    /** 调用券服务锁券并返回抵扣后应付金额 */
    @SuppressWarnings("unchecked")
    private BigDecimal lockCoupon(Long userCouponId, Long orderId, Long userId, BigDecimal amount, Long productId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userCouponId", userCouponId);
            body.put("orderId", orderId);
            body.put("userId", userId);
            body.put("amount", amount);
            body.put("productId", productId);
            Map<String, Object> resp = restTemplate.postForObject(couponBaseUrl + "/api/coupon/lock", body, Map.class);
            Object code = resp == null ? null : resp.get("code");
            Object data = resp == null ? null : resp.get("data");
            if (code != null && "200".equals(String.valueOf(code)) && data instanceof Map<?, ?> d) {
                return new BigDecimal(String.valueOf(d.get("payAmount")));
            }
            throw new BizException("优惠券不可用，请重新选择");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("锁券失败 couponId={}：{}", userCouponId, e.getMessage());
            throw new BizException("优惠券服务不可用，请稍后重试");
        }
    }
}
