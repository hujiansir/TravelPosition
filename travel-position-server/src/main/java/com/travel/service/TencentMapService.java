package com.travel.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.travel.common.BusinessException;
import com.travel.config.TencentMapProperties;
import com.travel.dto.GeocodeResult;
import com.travel.util.AdcodeUtil;
import com.travel.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯位置服务:逆地理编码(lat,lng → 地级市 adcode),带 Redis 缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentMapService {

    private final TencentMapProperties properties;
    private final RedisUtil redisUtil;

    private static final String CACHE_PREFIX = "geo:";
    private static final long CACHE_TTL = 30L * 24 * 3600;

    /**
     * 逆地理编码(带缓存)
     */
    public GeocodeResult reverseGeocode(double lat, double lng) {
        String cacheKey = buildCacheKey(lat, lng);
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            try {
                JSONObject json = JSONUtil.parseObj(cached.toString());
                return new GeocodeResult(
                        json.getLong("cityAdcode"),
                        json.getStr("cityName"),
                        json.getStr("provinceName"));
            } catch (Exception e) {
                log.warn("地理缓存解析失败,重新查询: {}", e.getMessage());
            }
        }
        GeocodeResult result = callTencentApi(lat, lng);
        Map<String, Object> map = new HashMap<>();
        map.put("cityAdcode", result.getCityAdcode());
        map.put("cityName", result.getCityName());
        map.put("provinceName", result.getProvinceName());
        redisUtil.set(cacheKey, JSONUtil.toJsonStr(map), CACHE_TTL);
        return result;
    }

    /**
     * 调腾讯位置服务逆地理编码 API(失败重试 1 次)
     */
    private GeocodeResult callTencentApi(double lat, double lng) {
        String url = String.format("%s?location=%s,%s&key=%s",
                properties.getGeocoderUrl(), lat, lng, properties.getKey());

        String body = null;
        for (int i = 0; i < 2; i++) {
            try {
                body = HttpUtil.get(url, 5000);
                break;
            } catch (Exception e) {
                log.warn("腾讯逆地理调用失败(第{}次): {}", i + 1, e.getMessage());
                if (i == 1) {
                    throw new BusinessException("定位服务繁忙,请重试");
                }
            }
        }

        JSONObject json = JSONUtil.parseObj(body);
        int status = json.getInt("status", -1);
        if (status != 0) {
            log.error("腾讯逆地理返回异常: {}", body);
            throw new BusinessException("定位服务繁忙,请重试");
        }

        JSONObject resultObj = json.getJSONObject("result");
        JSONObject adInfo = resultObj.getJSONObject("ad_info");
        long districtAdcode = adInfo.getLong("adcode");
        String cityName = adInfo.getStr("city");
        String provinceName = adInfo.getStr("province");
        long cityAdcode = AdcodeUtil.districtToCity(districtAdcode);
        // 直辖市 city 字段为空,回退用省名
        if (cityName == null || cityName.isEmpty()) {
            cityName = provinceName;
        }
        return new GeocodeResult(cityAdcode, cityName, provinceName);
    }

    private String buildCacheKey(double lat, double lng) {
        return CACHE_PREFIX + String.format("%.6f,%.6f", lat, lng);
    }

}
