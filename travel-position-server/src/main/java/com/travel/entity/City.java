package com.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 城市实体(地级市元数据)
 */
@Data
@TableName("travel.city")
public class City {

    /**
     * 6位行政区划代码(与DataV adcode一致,作为主键)
     */
    @TableId(type = IdType.INPUT)
    private Long adcode;

    /**
     * 城市名称
     */
    private String name;

    /**
     * 所属省adcode
     */
    private Long provinceAdcode;

    /**
     * 所属省名称
     */
    private String provinceName;

    /**
     * 中心点经度
     */
    private BigDecimal centerLng;

    /**
     * 中心点纬度
     */
    private BigDecimal centerLat;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
