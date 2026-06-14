import { request } from '../utils/request'

/**
 * 微信登录响应
 */
export interface WxLoginResponse {
  openid: string
  token?: string
  userInfo?: {
    id: number
    openid: string
    nickname: string
    avatarUrl: string
  }
}

/**
 * 手机号信息
 */
export interface PhoneNumberInfo {
  phoneNumber: string
  purePhoneNumber: string
  countryCode: string
}

/**
 * 微信授权 API
 */
export const wxAuthApi = {
  /**
   * 微信登录
   */
  login(code: string): Promise<WxLoginResponse> {
    return request.post('/wx/login', { code })
  },

  /**
   * 获取手机号
   */
  getPhoneNumber(code: string): Promise<PhoneNumberInfo> {
    return request.post('/wx/phone', { code })
  }
}
