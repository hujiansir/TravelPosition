package com.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {

    /**
     * 微信登录凭证 code
     */
    @NotBlank(message = "登录凭证不能为空")
    private String code;

}
