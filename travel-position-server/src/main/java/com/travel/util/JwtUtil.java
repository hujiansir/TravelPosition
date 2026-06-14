package com.travel.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.travel.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类(基于 Hutool)
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 签发 JWT
     */
    public String generate(Long userId, String openid) {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("openid", openid);
        payload.put("iat", now);
        payload.put("nbf", now);
        payload.put("exp", now + jwtProperties.getExpireSeconds());
        return JWTUtil.createToken(payload, jwtProperties.getSecret().getBytes());
    }

    /**
     * 校验签名与有效期
     */
    public boolean verify(String token) {
        try {
            byte[] key = jwtProperties.getSecret().getBytes();
            JWT jwt = JWTUtil.parseToken(token).setKey(key);
            return jwt.verify() && jwt.validate(0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 userId
     */
    public Long getUserId(String token) {
        try {
            Object userId = JWTUtil.parseToken(token).getPayload("userId");
            return Long.valueOf(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

}
