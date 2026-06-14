package com.travel.service;

import cn.hutool.http.HttpUtil;
import com.travel.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 地图 GeoJSON 代理服务
 * 前端 wx.request 直连 DataV.GeoAtlas 会被 Referer 防盗链拦截(403),
 * 改由后端代理拉取并缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapGeoService {

    private static final String DATAV_URL = "https://geo.datav.aliyun.com/areas_v3/bound/";
    private static final String CACHE_PREFIX = "geojson:";
    private static final long CACHE_TTL = 30L * 24 * 3600;

    private final RedisUtil redisUtil;

    /**
     * 获取行政区边界 GeoJSON(后端代理 + Redis 缓存)
     */
    public String getGeoJson(Long adcode) {
        String key = CACHE_PREFIX + adcode;
        Object cached = redisUtil.get(key);
        if (cached != null) {
            return cached.toString();
        }
        String url = DATAV_URL + adcode + "_full.json";
        log.info("代理拉取 DataV GeoJSON: {}", url);
        String body = HttpUtil.get(url, 10000);
        redisUtil.set(key, body, CACHE_TTL);
        return body;
    }

}
