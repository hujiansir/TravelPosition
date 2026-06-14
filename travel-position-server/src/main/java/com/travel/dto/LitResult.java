package com.travel.dto;

import lombok.Data;

import java.util.List;

/**
 * 已点亮城市集合 + 统计
 */
@Data
public class LitResult {

    private Integer litCityCount;

    private Integer litProvinceCount;

    private List<LitCityVO> cities;

}
