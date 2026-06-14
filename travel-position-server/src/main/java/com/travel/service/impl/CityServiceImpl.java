package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.City;
import com.travel.mapper.CityMapper;
import com.travel.service.CityService;
import org.springframework.stereotype.Service;

/**
 * 城市服务实现
 */
@Service
public class CityServiceImpl extends ServiceImpl<CityMapper, City> implements CityService {

    @Override
    public City getByAdcode(Long adcode) {
        if (adcode == null) {
            return null;
        }
        // adcode 即主键
        return this.getById(adcode);
    }

    @Override
    public City getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<City>().eq(City::getName, name).last("LIMIT 1"));
    }

}
