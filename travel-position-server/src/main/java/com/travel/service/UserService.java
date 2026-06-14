package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据 openid 查询用户
     */
    User getByOpenid(String openid);

    /**
     * 登录或注册:openid 存在则返回,不存在则创建
     */
    User loginOrRegister(String openid, String unionid);

}
