package com.travel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥
     */
    private String secret = "travel-position-default-secret-change-me";

    /**
     * 有效期(秒),默认 7 天
     */
    private long expireSeconds = 604800L;

}
