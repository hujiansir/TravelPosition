package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import com.travel.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByOpenid(String openid) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));
    }

}
