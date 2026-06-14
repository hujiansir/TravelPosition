package com.travel.controller;

import com.travel.common.Result;
import com.travel.dto.CityVO;
import com.travel.entity.City;
import com.travel.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 城市控制器
 */
@RestController
@RequestMapping("/city")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    /**
     * 根据 adcode 查询城市元数据
     */
    @GetMapping("/by-adcode")
    public Result<CityVO> getByAdcode(@RequestParam Long adcode) {
        City city = cityService.getByAdcode(adcode);
        if (city == null) {
            return Result.success(null);
        }
        CityVO vo = new CityVO();
        vo.setAdcode(city.getAdcode());
        vo.setName(city.getName());
        vo.setProvinceName(city.getProvinceName());
        vo.setCenterLng(city.getCenterLng());
        vo.setCenterLat(city.getCenterLat());
        return Result.success(vo);
    }

}
