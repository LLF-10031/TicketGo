package com.ticketgo.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ticketgo.common.constant.RedisKeys;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.util.JwtUtil;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.user.dto.CaptchaResponse;
import com.ticketgo.user.dto.LoginRequest;
import com.ticketgo.user.dto.RegisterRequest;
import com.ticketgo.user.dto.TokenResponse;
import com.ticketgo.user.entity.User;
import com.ticketgo.user.mapper.UserMapper;
import com.ticketgo.user.service.SmsService;
import com.ticketgo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;

    @Value("${ticketgo.jwt.access-ttl-seconds:900}")
    private long accessTtl;

    @Value("${ticketgo.jwt.refresh-ttl-seconds:604800}")
    private long refreshTtl;

    private static final long CAPTCHA_TTL_SECONDS = 300;
    private static final long SMS_TTL_SECONDS = 300;

    @Override
    public CaptchaResponse getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(160, 48, 4, 24);
        String code = captcha.getCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeys.CAPTCHA + captchaId, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);
        return new CaptchaResponse(captchaId, captcha.getImageBase64());
    }

    @Override
    public void sendSms(String phone, String captchaId, String captchaInput) {
        // 图形验证码前置校验（一次性）
        String key = RedisKeys.CAPTCHA + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null) {
            throw new BizException("图形验证码已过期，请刷新");
        }
        if (!expected.equalsIgnoreCase(captchaInput)) {
            throw new BizException("图形验证码错误");
        }
        redisTemplate.delete(key);

        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(RedisKeys.SMS_CODE + phone, code, SMS_TTL_SECONDS, TimeUnit.SECONDS);
        smsService.send(phone, code);
    }

    @Override
    public TokenResponse register(RegisterRequest req) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getPhone, req.phone()));
        if (count != null && count > 0) {
            throw new BizException("手机号已注册");
        }
        verifySmsIfPresent(req.phone(), req.code());

        User user = new User();
        user.setId(SnowflakeIdGenerator.generateId());
        user.setPhone(req.phone());
        if (StringUtils.hasText(req.password())) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        user.setNickName(StringUtils.hasText(req.nickName()) ? req.nickName() : "用户" + req.phone().substring(7));
        user.setIcon("");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        return issueTokens(user);
    }

    @Override
    public TokenResponse login(LoginRequest req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, req.phone()));
        if (user == null) {
            throw new BizException("用户不存在，请先注册");
        }
        if (StringUtils.hasText(req.password())) {
            if (!StringUtils.hasText(user.getPassword()) || !passwordEncoder.matches(req.password(), user.getPassword())) {
                throw new BizException("密码错误");
            }
        } else if (StringUtils.hasText(req.code())) {
            verifySmsIfPresent(req.phone(), req.code());
        } else {
            throw new BizException("请输入密码或验证码");
        }
        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Long userId;
        try {
            userId = jwtUtil.getUserId(refreshToken);
        } catch (Exception e) {
            throw new BizException("刷新令牌无效");
        }
        String stored = redisTemplate.opsForValue().get(RedisKeys.REFRESH_TOKEN + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BizException("刷新令牌已失效，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String newAccess = jwtUtil.generateAccessToken(userId);
        redisTemplate.opsForValue().set(RedisKeys.LOGIN_TOKEN + userId, newAccess, accessTtl, TimeUnit.SECONDS);
        return new TokenResponse(newAccess, refreshToken, user.getId(), user.getNickName(), user.getIcon());
    }

    @Override
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        String hash = DigestUtil.md5Hex(accessToken);
        redisTemplate.opsForValue().set(RedisKeys.TOKEN_BLACKLIST + hash, "1", accessTtl, TimeUnit.SECONDS);
        try {
            Long userId = jwtUtil.getUserId(accessToken);
            redisTemplate.delete(RedisKeys.REFRESH_TOKEN + userId);
            redisTemplate.delete(RedisKeys.LOGIN_TOKEN + userId);
        } catch (Exception ignored) {
        }
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickName) {
        User user = new User();
        user.setId(userId);
        user.setNickName(nickName);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = new User();
        user.setId(userId);
        user.setIcon(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (StringUtils.hasText(user.getPassword()) && !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    private void verifySmsIfPresent(String phone, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("请输入短信验证码");
        }
        String key = RedisKeys.SMS_CODE + phone;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(code)) {
            throw new BizException("短信验证码错误或已过期");
        }
        redisTemplate.delete(key);
    }

    private TokenResponse issueTokens(User user) {
        Long userId = user.getId();
        String access = jwtUtil.generateAccessToken(userId);
        String refresh = jwtUtil.generateRefreshToken(userId);
        redisTemplate.opsForValue().set(RedisKeys.LOGIN_TOKEN + userId, access, accessTtl, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(RedisKeys.REFRESH_TOKEN + userId, refresh, refreshTtl, TimeUnit.SECONDS);
        return new TokenResponse(access, refresh, userId, user.getNickName(), user.getIcon());
    }
}
