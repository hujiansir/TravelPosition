package com.travel.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已点亮城市
 */
@Data
public class LitCityVO {

    private Long cityAdcode;

    private String cityName;

    private String provinceName;

    /**
     * 城市中心经度(marker 定位)
     */
    private BigDecimal centerLng;

    /**
     * 城市中心纬度
     */
    private BigDecimal centerLat;

    /**
     * 首次打卡时间
     */
    private LocalDateTime firstCheckinTime;

}
