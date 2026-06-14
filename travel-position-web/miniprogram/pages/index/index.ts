// 版图主页
import { checkinApi, LitResult } from '../../api/checkin'
import {
  loadChinaGeoJson,
  loadGeoJson,
  geoJsonToPolygons,
  MapPolygon,
  MUNICIPALITIES
} from '../../utils/geo'

Page({
  data: {
    polygons: [] as MapPolygon[],
    litCityCount: 0,
    litProvinceCount: 0,
    // 中国中心
    center: { latitude: 35.0, longitude: 105.0 },
    scale: 4,
    loading: true
  },

  onShow() {
    // 等登录完成(拿到 token)再加载版图数据,避免 token 未就绪导致的 401
    const app = getApp<IAppOption>()
    app.wxLogin().then(() => {
      this.loadData()
    }).catch(() => {
      this.loadData()
    })
  },

  /** 加载已点亮数据并渲染版图 */
  loadData() {
    this.setData({ loading: true })
    checkinApi.lit().then((result: LitResult) => {
      this.renderMap(result)
    }).catch(() => {
      this.renderMap({ litCityCount: 0, litProvinceCount: 0, cities: [] })
    })
  },

  renderMap(result: LitResult) {
    const litAdcodes = new Set<number>(result.cities.map((c) => c.cityAdcode))
    this.setData({
      litCityCount: result.litCityCount,
      litProvinceCount: result.litProvinceCount
    })

    loadChinaGeoJson().then((chinaFc) => {
      // 省级骨架:所有省深色填充盖住底图;直辖市点亮用青蓝填充
      const basePolygons = geoJsonToPolygons(
        chinaFc,
        (ad) => MUNICIPALITIES.has(ad) && litAdcodes.has(ad)
      )

      // 已点亮普通城市所在省(直辖市除外)
      const provinceSet = new Set<number>()
      result.cities.forEach((c) => {
        const prov = Math.floor(c.cityAdcode / 10000) * 10000
        if (!MUNICIPALITIES.has(prov)) {
          provinceSet.add(prov)
        }
      })

      const loads: Promise<any>[] = Array.from(provinceSet).map((ad) => loadGeoJson(ad))
      Promise.all(loads).then((fcs) => {
        const cityPolygons: MapPolygon[] = []
        fcs.forEach((fc) => {
          // 市级只渲染已点亮城市(青蓝高亮填充)
          cityPolygons.push(...geoJsonToPolygons(fc, (ad) => litAdcodes.has(ad), false))
        })
        this.setData({
          polygons: [...basePolygons, ...cityPolygons],
          loading: false
        })
      }).catch(() => {
        this.setData({ polygons: basePolygons, loading: false })
      })
    }).catch((err) => {
      console.error('[index] 中国底图加载失败:', err)
      this.setData({ loading: false })
    })
  },

  /** 进入打卡页 */
  goCheckin() {
    wx.navigateTo({ url: '/pages/checkin/checkin' })
  }
})
