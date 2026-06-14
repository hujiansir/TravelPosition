// app.ts
import { wxAuthApi } from './api/wx-auth'

App<IAppOption>({
  globalData: {
    userInfo: null,
    token: ''
  },

  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 微信登录
    this.wxLogin()
  },

  /**
   * 微信登录
   */
  wxLogin() {
    wx.login({
      success: async (res) => {
        if (res.code) {
          try {
            const result = await wxAuthApi.login(res.code)
            console.log('登录成功', result)

            // 保存 token（如果后端返回）
            if (result.token) {
              this.globalData.token = result.token
              wx.setStorageSync('token', result.token)
            }

            // 保存 openid
            wx.setStorageSync('openid', result.openid)
          } catch (error) {
            console.error('登录失败', error)
            wx.showToast({
              title: '登录失败',
              icon: 'none'
            })
          }
        } else {
          console.error('获取登录凭证失败', res.errMsg)
        }
      },
      fail: (err) => {
        console.error('wx.login 调用失败', err)
      }
    })
  }
})