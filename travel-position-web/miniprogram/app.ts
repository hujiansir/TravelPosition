// app.ts
import { wxAuthApi } from './api/wx-auth'

App<IAppOption>({
  globalData: {
    userInfo: null,
    token: ''
  },

  // 登录并发锁:重复调用复用同一 Promise,避免并发产生多个 /wx/login
  loginPromise: null as Promise<void> | null,

  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 微信登录
    this.wxLogin()
  },

  /**
   * 微信登录(带并发锁)
   */
  wxLogin(): Promise<void> {
    if (this.loginPromise) {
      return this.loginPromise
    }
    this.loginPromise = new Promise<void>((resolve, reject) => {
      wx.login({
        success: async (res) => {
          if (!res.code) {
            console.error('获取登录凭证失败', res.errMsg)
            reject(new Error(res.errMsg))
            return
          }
          try {
            const result = await wxAuthApi.login(res.code)
            console.log('登录成功', result)

            // 保存 token
            if (result.token) {
              this.globalData.token = result.token
              wx.setStorageSync('token', result.token)
            }

            // 保存 openid
            wx.setStorageSync('openid', result.openid)

            // 保存用户信息
            if (result.userInfo) {
              wx.setStorageSync('userInfo', result.userInfo)
            }

            resolve()
          } catch (error) {
            console.error('登录失败', error)
            wx.showToast({
              title: '登录失败',
              icon: 'none'
            })
            reject(error)
          }
        },
        fail: (err) => {
          console.error('wx.login 调用失败', err)
          reject(err)
        }
      })
    })
    // 登录结束(无论成败)清理锁,允许后续重新登录
    this.loginPromise.then(() => {
      this.loginPromise = null
    }).catch(() => {
      this.loginPromise = null
    })
    return this.loginPromise
  }
})
