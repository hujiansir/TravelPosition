package com.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 打卡足迹实体
 */
@Data
@TableName("travel.checkin")
public class Checkin {

    /**
     * 打卡记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 打卡城市adcode
     */
    private Long cityAdcode;

    /**
     * 打卡经度
     */
    private BigDecimal lng;

    /**
     * 打卡纬度
     */
    private BigDecimal lat;

    /**
     * 文字备注
     */
    private String note;

    /**
     * 打卡时间
     */
    private LocalDateTime checkinTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;

}
