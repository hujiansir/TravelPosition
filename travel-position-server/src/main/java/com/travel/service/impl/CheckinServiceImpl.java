package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.common.BusinessException;
import com.travel.dto.CheckinRecordVO;
import com.travel.dto.CheckinRequest;
import com.travel.dto.CheckinVO;
import com.travel.dto.GeocodeResult;
import com.travel.dto.LitCityVO;
import com.travel.dto.LitResult;
import com.travel.dto.PageResult;
import com.travel.entity.Checkin;
import com.travel.entity.City;
import com.travel.mapper.CheckinMapper;
import com.travel.service.CheckinService;
import com.travel.service.CityService;
import com.travel.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打卡足迹服务实现
 */
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements CheckinService {

    private final TencentMapService tencentMapService;
    private final CityService cityService;

    @Override
    public CheckinVO checkin(Long userId, CheckinRequest request) {
        // 1. 逆地理编码
        GeocodeResult geo = tencentMapService.reverseGeocode(request.getLat(), request.getLng());

        // 2. 城市匹配:adcode 优先,名称回退(直辖市/省直辖县级市)
        City city = cityService.getByAdcode(geo.getCityAdcode());
        if (city == null) {
            city = cityService.getByName(geo.getCityName());
        }
        if (city == null) {
            throw new BusinessException("当前定位未匹配到地级市");
        }

        // 3. 判断是否首次点亮
        long existCount = this.count(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCityAdcode, city.getAdcode()));
        boolean isFirstLit = existCount == 0;

        // 4. 写入打卡记录
        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setCityAdcode(city.getAdcode());
        checkin.setLng(BigDecimal.valueOf(request.getLng()));
        checkin.setLat(BigDecimal.valueOf(request.getLat()));
        checkin.setNote(request.getNote());
        checkin.setCheckinTime(LocalDateTime.now());
        this.save(checkin);

        // 5. 返回
        CheckinVO vo = new CheckinVO();
        vo.setCheckinId(checkin.getId());
        vo.setCityAdcode(city.getAdcode());
        vo.setCityName(city.getName());
        vo.setProvinceName(city.getProvinceName());
        vo.setIsFirstLit(isFirstLit);
        return vo;
    }

    @Override
    public LitResult lit(Long userId) {
        // 按时间升序取该用户全部打卡,首个出现即首次点亮时间
        List<Checkin> all = this.list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByAsc(Checkin::getCheckinTime));

        Map<Long, LocalDateTime> firstTimeMap = new LinkedHashMap<>();
        for (Checkin c : all) {
            firstTimeMap.putIfAbsent(c.getCityAdcode(), c.getCheckinTime());
        }

        List<LitCityVO> cities = new ArrayList<>();
        Set<Long> provinceSet = new HashSet<>();
        for (Map.Entry<Long, LocalDateTime> entry : firstTimeMap.entrySet()) {
            City city = cityService.getByAdcode(entry.getKey());
            if (city == null) {
                continue;
            }
            LitCityVO vo = new LitCityVO();
            vo.setCityAdcode(city.getAdcode());
            vo.setCityName(city.getName());
            vo.setProvinceName(city.getProvinceName());
            vo.setCenterLng(city.getCenterLng());
            vo.setCenterLat(city.getCenterLat());
            vo.setFirstCheckinTime(entry.getValue());
            cities.add(vo);
            provinceSet.add(city.getProvinceAdcode());
        }

        LitResult result = new LitResult();
        result.setLitCityCount(cities.size());
        result.setLitProvinceCount(provinceSet.size());
        result.setCities(cities);
        return result;
    }

    @Override
    public PageResult<CheckinRecordVO> list(Long userId, int page, int size) {
        Page<Checkin> p = new Page<>(page, size);
        Page<Checkin> result = this.page(p, new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCheckinTime));

        Set<Long> adcodes = result.getRecords().stream()
                .map(Checkin::getCityAdcode)
                .collect(Collectors.toSet());
        Map<Long, City> cityMap = new HashMap<>();
        for (Long ad : adcodes) {
            City city = cityService.getByAdcode(ad);
            if (city != null) {
                cityMap.put(ad, city);
            }
        }

        List<CheckinRecordVO> records = result.getRecords().stream().map(c -> {
            CheckinRecordVO vo = new CheckinRecordVO();
            vo.setId(c.getId());
            vo.setCityAdcode(c.getCityAdcode());
            City city = cityMap.get(c.getCityAdcode());
            vo.setCityName(city != null ? city.getName() : null);
            vo.setProvinceName(city != null ? city.getProvinceName() : null);
            vo.setLng(c.getLng() != null ? c.getLng().doubleValue() : null);
            vo.setLat(c.getLat() != null ? c.getLat().doubleValue() : null);
            vo.setNote(c.getNote());
            vo.setCheckinTime(c.getCheckinTime());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), records, page, size);
    }

    @Override
    public void remove(Long userId, Long checkinId) {
        Checkin checkin = this.getById(checkinId);
        if (checkin == null || !userId.equals(checkin.getUserId())) {
            throw new BusinessException("打卡记录不存在");
        }
        this.removeById(checkinId);
    }

}
