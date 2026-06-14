// 打卡页
import { checkinApi, CheckinVO } from '../../api/checkin'

Page({
  data: {
    lng: 0,
    lat: 0,
    note: '',
    locationReady: false,
    locating: true,
    submitting: false
  },

  onLoad() {
    this.getLocation()
  },

  /** 获取当前定位(gcj02) */
  getLocation() {
    this.setData({ locating: true })
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          lat: res.latitude,
          lng: res.longitude,
          locationReady: true,
          locating: false
        })
      },
      fail: () => {
        this.setData({ locating: false })
        wx.showModal({
          title: '定位失败',
          content: '需要定位权限才能打卡,请前往设置开启',
          confirmText: '去设置',
          success: (res) => {
            if (res.confirm) {
              wx.openSetting()
            }
          }
        })
      }
    })
  },

  onNoteInput(e: any) {
    this.setData({ note: e.detail.value })
  },

  /** 提交打卡 */
  doCheckin() {
    if (!this.data.locationReady) {
      wx.showToast({ title: '定位中,请稍候', icon: 'none' })
      return
    }
    if (this.data.submitting) {
      return
    }
    this.setData({ submitting: true })
    checkinApi.create({
      lng: this.data.lng,
      lat: this.data.lat,
      note: this.data.note
    }).then((res: CheckinVO) => {
      const msg = res.isFirstLit
        ? '🎉 首次点亮 ' + res.cityName + '！'
        : '已在 ' + res.cityName + ' 留下足迹'
      wx.showToast({ title: msg, icon: 'none', duration: 2500 })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }).catch(() => {
      this.setData({ submitting: false })
    })
  }
})
