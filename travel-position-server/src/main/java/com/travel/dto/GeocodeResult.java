package com.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逆地理编码结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResult {

    /**
     * 换算后的地级市 adcode
     */
    private Long cityAdcode;

    /**
     * 城市名称(直辖市为省名)
     */
    private String cityName;

    /**
     * 省份名称
     */
    private String provinceName;

}
