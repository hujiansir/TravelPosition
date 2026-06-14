package com.travel.controller;

import com.travel.common.Result;
import com.travel.service.MapGeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地图数据控制器(代理 DataV GeoJSON)
 */
@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapGeoController {

    private final MapGeoService mapGeoService;

    /**
     * 获取行政区边界 GeoJSON
     */
    @GetMapping("/geojson")
    public Result<String> getGeoJson(@RequestParam Long adcode) {
        return Result.success(mapGeoService.getGeoJson(adcode));
    }

}
