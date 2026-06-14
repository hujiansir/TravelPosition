import { request } from '../utils/request'

/**
 * 城市元数据
 */
export interface CityVO {
  adcode: number
  name: string
  provinceName: string
  centerLng: number
  centerLat: number
}

/**
 * 城市 API
 */
export const cityApi = {
  /** 根据 adcode 查询城市 */
  getByAdcode(adcode: number): Promise<CityVO> {
    return request.get('/city/by-adcode', { adcode })
  }
}
