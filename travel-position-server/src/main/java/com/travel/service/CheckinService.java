package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.dto.CheckinRecordVO;
import com.travel.dto.CheckinRequest;
import com.travel.dto.CheckinVO;
import com.travel.dto.LitResult;
import com.travel.dto.PageResult;
import com.travel.entity.Checkin;

/**
 * 打卡足迹服务接口
 */
public interface CheckinService extends IService<Checkin> {

    /**
     * 打卡
     */
    CheckinVO checkin(Long userId, CheckinRequest request);

    /**
     * 已点亮城市 + 统计
     */
    LitResult lit(Long userId);

    /**
     * 足迹时间线分页
     */
    PageResult<CheckinRecordVO> list(Long userId, int page, int size);

    /**
     * 软删一条足迹
     */
    void remove(Long userId, Long checkinId);

}
