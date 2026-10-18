package com.ticketgo.user.config;

import com.ticketgo.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    @Value("${ticketgo.jwt.secret}")
    private String jwtSecret;

    @Value("${ticketgo.jwt.access-ttl-seconds:900}")
    private long accessTtl;

    @Value("${ticketgo.jwt.refresh-ttl-seconds:604800}")
    private long refreshTtl;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, accessTtl, refreshTtl);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
