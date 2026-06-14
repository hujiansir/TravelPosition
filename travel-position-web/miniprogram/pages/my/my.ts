// 个人页
import { checkinApi, LitResult } from '../../api/checkin'

Page({
  data: {
    nickname: '旅行者',
    avatarUrl: '',
    defaultAvatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    litCityCount: 0,
    litProvinceCount: 0
  },

  onShow() {
    this.loadUserInfo()
    this.loadStats()
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo) {
      this.setData({
        nickname: userInfo.nickname || '旅行者',
        avatarUrl: userInfo.avatarUrl || ''
      })
    }
  },

  loadStats() {
    checkinApi.lit().then((res: LitResult) => {
      this.setData({
        litCityCount: res.litCityCount,
        litProvinceCount: res.litProvinceCount
      })
    })
  }
})
