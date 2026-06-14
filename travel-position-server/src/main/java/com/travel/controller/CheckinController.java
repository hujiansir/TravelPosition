package com.travel.controller;

import com.travel.common.Result;
import com.travel.common.UserContext;
import com.travel.dto.CheckinRecordVO;
import com.travel.dto.CheckinRequest;
import com.travel.dto.CheckinVO;
import com.travel.dto.LitResult;
import com.travel.dto.PageResult;
import com.travel.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打卡足迹控制器
 */
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * 打卡
     */
    @PostMapping
    public Result<CheckinVO> checkin(@Validated @RequestBody CheckinRequest request) {
        Long userId = UserContext.getUserId();
        return Result.success(checkinService.checkin(userId, request));
    }

    /**
     * 已点亮城市 + 统计
     */
    @GetMapping("/lit")
    public Result<LitResult> lit() {
        Long userId = UserContext.getUserId();
        return Result.success(checkinService.lit(userId));
    }

    /**
     * 足迹时间线分页
     */
    @GetMapping("/list")
    public Result<PageResult<CheckinRecordVO>> list(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(checkinService.list(userId, page, size));
    }

    /**
     * 软删一条足迹
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        checkinService.remove(userId, id);
        return Result.success();
    }

}
