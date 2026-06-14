// 足迹页
import { checkinApi, CheckinRecordVO } from '../../api/checkin'

Page({
  data: {
    records: [] as CheckinRecordVO[],
    page: 1,
    size: 20,
    total: 0,
    loading: false,
    noMore: false
  },

  onShow() {
    // 每次进入重新加载(打卡后可能有新记录)
    this.setData({ records: [], page: 1, noMore: false })
    this.loadList()
  },

  loadList() {
    if (this.data.loading || this.data.noMore) {
      return
    }
    this.setData({ loading: true })
    checkinApi.list(this.data.page, this.data.size).then((res) => {
      const records = this.data.page === 1
        ? res.records
        : [...this.data.records, ...res.records]
      this.setData({
        records,
        total: res.total,
        loading: false,
        noMore: records.length >= res.total
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  onReachBottom() {
    if (this.data.noMore || this.data.loading) {
      return
    }
    this.setData({ page: this.data.page + 1 })
    this.loadList()
  },

  onDelete(e: any) {
    const id = e.currentTarget.dataset.id as number
    wx.showModal({
      title: '删除足迹',
      content: '确定删除这条足迹吗?',
      success: (res) => {
        if (res.confirm) {
          checkinApi.remove(id).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.setData({ records: [], page: 1, noMore: false })
            this.loadList()
          })
        }
      }
    })
  }
})
