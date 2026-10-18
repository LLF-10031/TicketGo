package com.ticketgo.coupon.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketgo.common.claim.ClaimResult;
import com.ticketgo.common.claim.StockClaimEngine;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.result.PageResult;
import com.ticketgo.common.result.ResultCode;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.coupon.dto.AvailableCoupon;
import com.ticketgo.coupon.dto.CouponAvailableRequest;
import com.ticketgo.coupon.dto.CouponBuyRequest;
import com.ticketgo.coupon.dto.CouponLockRequest;
import com.ticketgo.coupon.dto.CouponLockResult;
import com.ticketgo.coupon.entity.CouponTemplate;
import com.ticketgo.coupon.entity.Order;
import com.ticketgo.coupon.entity.OrderItem;
import com.ticketgo.coupon.entity.UserCoupon;
import com.ticketgo.coupon.mapper.CouponTemplateMapper;
import com.ticketgo.coupon.mapper.OrderItemMapper;
import com.ticketgo.coupon.mapper.OrderMapper;
import com.ticketgo.coupon.mapper.UserCouponMapper;
import com.ticketgo.coupon.service.CouponService;
import com.ticketgo.coupon.strategy.CouponStrategy;
import com.ticketgo.coupon.strategy.CouponStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 优惠券：限量领取复用通用领取引擎；券种计算走策略模式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockClaimEngine stockClaimEngine;
    private final CouponStrategyFactory strategyFactory;

    private static final int PAY_EXPIRE_MINUTES = 15;

    @Override
    public List<CouponTemplate> templates(Integer scopeType, Integer status) {
        var wrapper = Wrappers.<CouponTemplate>lambdaQuery();
        if (scopeType != null) {
            wrapper.eq(CouponTemplate::getScopeType, scopeType);
        }
        if (status != null) {
            wrapper.eq(CouponTemplate::getStatus, status);
        }
        wrapper.orderByDesc(CouponTemplate::getCreateTime);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public Long receive(Long userId, Long templateId) {
        CouponTemplate template = getTemplate(templateId);
        if (template.getStockType() != null && template.getStockType() == 1) {
            throw new BizException("该券为购买券，请走购买流程");
        }
        int perUserLimit = template.getPerUserLimit() == null || template.getPerUserLimit() <= 0
                ? 1 : template.getPerUserLimit();
        ClaimResult claim = stockClaimEngine.claim(
                RedisKeys.couponStock(templateId),
                RedisKeys.COUPON_USER_COUNT + templateId,
                userId,
                perUserLimit);
        if (!claim.isSuccess()) {
            throw new BizException(toResultCode(claim));
        }
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(SnowflakeIdGenerator.generateId());
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setStatus(0);
        userCoupon.setReceivedAt(now);
        userCoupon.setExpireAt(computeExpire(template, now));
        userCoupon.setCreateTime(now);
        userCoupon.setUpdateTime(now);
        userCouponMapper.insert(userCoupon);
        log.info("领券成功 userId={} templateId={} userCouponId={}", userId, templateId, userCoupon.getId());
        return userCoupon.getId();
    }

    @Override
    public Long buy(Long userId, CouponBuyRequest req) {
        if (req == null || req.templateId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        CouponTemplate template = getTemplate(req.templateId());
        int quantity = req.quantity() == null || req.quantity() <= 0 ? 1 : req.quantity();
        int perUserLimit = template.getPerUserLimit() == null || template.getPerUserLimit() <= 0
                ? 1 : template.getPerUserLimit();
        if (quantity > perUserLimit) {
            throw new BizException("超过每人限购数量");
        }
        BigDecimal price = template.getPrice() == null ? BigDecimal.ZERO : template.getPrice();
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setId(SnowflakeIdGenerator.generateId());
        order.setOrderNo("CP" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6));
        order.setUserId(userId);
        order.setOrderType(2);
        order.setSessionId(null);
        order.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0);
        order.setPayExpireAt(now.plusMinutes(PAY_EXPIRE_MINUTES));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        // 券订单明细：tier_id 复用为 templateId，供支付成功后发券
        OrderItem item = new OrderItem();
        item.setId(SnowflakeIdGenerator.generateId());
        item.setOrderId(order.getId());
        item.setTierId(template.getId());
        item.setSeatId(null);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setCreateTime(now);
        orderItemMapper.insert(item);

        log.info("购买券下单成功 userId={} templateId={} quantity={} orderNo={}",
                userId, req.templateId(), quantity, order.getOrderNo());
        return order.getId();
    }

    @Override
    public PageResult<UserCoupon> my(Long userId, String status, int page, int pageSize) {
        Page<UserCoupon> p = new Page<>(Math.max(page, 1), pageSize <= 0 ? 20 : pageSize);
        var wrapper = Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, userId);
        Integer statusCode = parseStatus(status);
        if (statusCode != null) {
            wrapper.eq(UserCoupon::getStatus, statusCode);
        }
        wrapper.orderByDesc(UserCoupon::getReceivedAt);
        userCouponMapper.selectPage(p, wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    @Override
    public List<AvailableCoupon> available(Long userId, CouponAvailableRequest req) {
        if (req == null || req.amount() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0));
        LocalDateTime now = LocalDateTime.now();
        List<AvailableCoupon> result = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(now)) {
                continue;
            }
            CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
            if (template == null || !inScope(template, req.productId())) {
                continue;
            }
            CouponStrategy strategy = strategyFactory.get(template.getType());
            if (strategy == null || !strategy.applicable(req.amount(), template)) {
                continue;
            }
            BigDecimal payAmount = strategy.calculate(req.amount(), template);
            BigDecimal discount = req.amount().subtract(payAmount);
            result.add(new AvailableCoupon(coupon.getId(), template.getId(), template.getName(),
                    template.getType(), discount, payAmount));
        }
        result.sort(Comparator.comparing(AvailableCoupon::payAmount));
        return result;
    }

    @Override
    public CouponLockResult lock(CouponLockRequest req) {
        if (req == null || req.userCouponId() == null || req.orderId() == null || req.amount() == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        UserCoupon coupon = userCouponMapper.selectById(req.userCouponId());
        if (coupon == null) {
            throw new BizException("优惠券不存在");
        }
        if (req.userId() != null && !req.userId().equals(coupon.getUserId())) {
            throw new BizException("优惠券不属于当前用户");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != 0) {
            throw new BizException("优惠券不可用");
        }
        if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
        if (template == null) {
            throw new BizException("券模板不存在");
        }
        if (!inScope(template, req.productId())) {
            throw new BizException("该券不适用于当前演出");
        }
        CouponStrategy strategy = strategyFactory.get(template.getType());
        if (strategy == null || !strategy.applicable(req.amount(), template)) {
            throw new BizException("未达到优惠券使用条件");
        }
        BigDecimal payAmount = strategy.calculate(req.amount(), template);
        BigDecimal discount = req.amount().subtract(payAmount);

        coupon.setStatus(1); // 锁定
        coupon.setLockOrderId(req.orderId());
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
        return new CouponLockResult(payAmount, discount);
    }

    @Override
    public void issue(Long orderId) {
        if (orderId == null) {
            return;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getOrderType() == null || order.getOrderType() != 2) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        LocalDateTime now = LocalDateTime.now();
        for (OrderItem item : items) {
            CouponTemplate template = templateMapper.selectById(item.getTierId());
            if (template == null) {
                continue;
            }
            Long exist = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                    .eq(UserCoupon::getSourceOrderId, orderId)
                    .eq(UserCoupon::getTemplateId, template.getId())
                    .eq(UserCoupon::getUserId, order.getUserId()));
            if (exist != null && exist > 0) {
                continue; // 幂等
            }
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            for (int i = 0; i < qty; i++) {
                UserCoupon uc = new UserCoupon();
                uc.setId(SnowflakeIdGenerator.generateId());
                uc.setUserId(order.getUserId());
                uc.setTemplateId(template.getId());
                uc.setStatus(0);
                uc.setSourceOrderId(orderId);
                uc.setReceivedAt(now);
                uc.setExpireAt(computeExpire(template, now));
                uc.setCreateTime(now);
                uc.setUpdateTime(now);
                userCouponMapper.insert(uc);
            }
        }
        log.info("券订单发券完成 orderId={}", orderId);
    }

    @Override
    public CouponTemplate createTemplate(CouponTemplate template) {
        if (template == null || !StringUtils.hasText(template.getName()) || !StringUtils.hasText(template.getType())) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        template.setId(SnowflakeIdGenerator.generateId());
        if (template.getStatus() == null) {
            template.setStatus(0);
        }
        template.setCreateTime(now);
        template.setUpdateTime(now);
        templateMapper.insert(template);
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(Long id, CouponTemplate template) {
        CouponTemplate exist = getTemplate(id);
        template.setId(exist.getId());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return templateMapper.selectById(id);
    }

    @Override
    public void publish(Long id) {
        CouponTemplate template = getTemplate(id);
        template.setStatus(1);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        // 限量券上架时预热库存到 Redis（供通用领取引擎原子扣减）
        if (template.getStockType() != null && template.getStockType() == 0
                && template.getTotalQty() != null && template.getTotalQty() > 0) {
            stockClaimEngine.warmUp(RedisKeys.couponStock(id), template.getTotalQty());
            log.info("券库存预热 templateId={} qty={}", id, template.getTotalQty());
        }
    }

    private CouponTemplate getTemplate(Long id) {
        CouponTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return template;
    }

    private boolean inScope(CouponTemplate template, Long productId) {
        if (template.getScopeType() == null || template.getScopeType() == 0) {
            return true;
        }
        if (productId == null || !StringUtils.hasText(template.getScopeProductIds())) {
            return false;
        }
        return Arrays.stream(template.getScopeProductIds().split(","))
                .map(String::trim)
                .anyMatch(s -> s.equals(String.valueOf(productId)));
    }

    private LocalDateTime computeExpire(CouponTemplate template, LocalDateTime now) {
        if (template.getValidType() != null && template.getValidType() == 1 && template.getValidDays() != null) {
            return now.plusDays(template.getValidDays());
        }
        return template.getValidEnd();
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "unused" -> 0;
            case "locked" -> 1;
            case "used" -> 2;
            case "released" -> 3;
            case "expired" -> 4;
            default -> {
                try {
                    yield Integer.valueOf(status);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private ResultCode toResultCode(ClaimResult claim) {
        return switch (claim) {
            case STOCK_EMPTY -> ResultCode.STOCK_EMPTY;
            case LIMIT_REACHED -> ResultCode.ALREADY_BOUGHT;
            case NOT_WARMED -> ResultCode.NOT_ON_SALE;
            default -> ResultCode.FAIL;
        };
    }
}
