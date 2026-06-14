package com.travel.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡足迹记录(时间线条目)
 */
@Data
public class CheckinRecordVO {

    private Long id;

    private Long cityAdcode;

    private String cityName;

    private String provinceName;

    private Double lng;

    private Double lat;

    private String note;

    private LocalDateTime checkinTime;

}
