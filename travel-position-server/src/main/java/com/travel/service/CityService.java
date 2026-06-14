package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.City;

/**
 * 城市服务接口
 */
public interface CityService extends IService<City> {

    /**
     * 根据 adcode 查询城市
     */
    City getByAdcode(Long adcode);

    /**
     * 根据名称查询城市(用于直辖市/省直辖市的名称回退)
     */
    City getByName(String name);

}
