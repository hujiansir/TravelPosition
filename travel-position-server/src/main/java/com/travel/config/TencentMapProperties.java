package com.travel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯位置服务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.map")
public class TencentMapProperties {

    /**
     * API key(需在腾讯位置服务后台申请,并授权小程序 appid)
     */
    private String key;

    /**
     * 逆地理编码接口地址
     */
    private String geocoderUrl = "https://apis.map.qq.com/ws/geocoder/v1/";

}
