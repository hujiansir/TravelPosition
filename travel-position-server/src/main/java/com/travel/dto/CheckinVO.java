package com.travel.dto;

import lombok.Data;

/**
 * 打卡返回结果
 */
@Data
public class CheckinVO {

    private Long checkinId;

    private Long cityAdcode;

    private String cityName;

    private String provinceName;

    /**
     * 是否首次点亮该城市
     */
    private Boolean isFirstLit;

}
