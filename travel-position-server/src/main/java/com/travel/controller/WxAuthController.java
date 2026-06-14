package com.travel.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.travel.common.BusinessException;
import com.travel.common.Result;
import com.travel.dto.WxLoginRequest;
import com.travel.entity.User;
import com.travel.service.UserService;
import com.travel.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx")
@RequiredArgsConstructor
public class WxAuthController {

    private final WxMaService wxMaService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 微信小程序登录:code → openid → 查/建用户 → 签发 JWT
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody WxLoginRequest request) {
        try {
            WxMaJscode2SessionResult session = wxMaService.getUserService()
                    .getSessionInfo(request.getCode());

            String openid = session.getOpenid();
            String unionid = session.getUnionid();
            log.info("微信登录成功,openid: {}", openid);

            // 查询或创建用户
            User user = userService.loginOrRegister(openid, unionid);

            // 签发 JWT
            String token = jwtUtil.generate(user.getId(), openid);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("openid", openid);
            userInfo.put("nickname", user.getNickname());
            userInfo.put("avatarUrl", user.getAvatarUrl());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("openid", openid);
            result.put("userInfo", userInfo);

            return Result.success(result);
        } catch (WxErrorException e) {
            log.error("微信登录失败:{}", e.getMessage(), e);
            throw new BusinessException("微信登录失败:" + e.getMessage());
        }
    }

    /**
     * 获取用户手机号
     */
    @PostMapping("/phone")
    public Result<WxMaPhoneNumberInfo> getPhoneNumber(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.isEmpty()) {
            throw new BusinessException("code 不能为空");
        }

        try {
            WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService()
                    .getPhoneNoInfo(code);
            log.info("获取手机号成功:{}", phoneInfo.getPhoneNumber());
            return Result.success(phoneInfo);
        } catch (WxErrorException e) {
            log.error("获取手机号失败:{}", e.getMessage(), e);
            throw new BusinessException("获取手机号失败:" + e.getMessage());
        }
    }

}
