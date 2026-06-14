package com.travel.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 城市视图对象
 */
@Data
public class CityVO {

    private Long adcode;

    private String name;

    private String provinceName;

    private BigDecimal centerLng;

    private BigDecimal centerLat;

}
