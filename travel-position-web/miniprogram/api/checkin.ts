import { request } from '../utils/request'

/**
 * 打卡返回
 */
export interface CheckinVO {
  checkinId: number
  cityAdcode: number
  cityName: string
  provinceName: string
  isFirstLit: boolean
}

/**
 * 已点亮城市
 */
export interface LitCityVO {
  cityAdcode: number
  cityName: string
  provinceName: string
  centerLng: number
  centerLat: number
  firstCheckinTime: string
}

/**
 * 点亮统计 + 集合
 */
export interface LitResult {
  litCityCount: number
  litProvinceCount: number
  cities: LitCityVO[]
}

/**
 * 打卡足迹记录
 */
export interface CheckinRecordVO {
  id: number
  cityAdcode: number
  cityName: string
  provinceName: string
  lng: number
  lat: number
  note: string
  checkinTime: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  total: number
  records: T[]
  page: number
  size: number
}

/**
 * 打卡入参
 */
export interface CheckinParams {
  lng: number
  lat: number
  note?: string
}

/**
 * 打卡足迹 API
 */
export const checkinApi = {
  /** 打卡 */
  create(data: CheckinParams): Promise<CheckinVO> {
    return request.post('/checkin', data)
  },

  /** 已点亮城市 + 统计 */
  lit(): Promise<LitResult> {
    return request.get('/checkin/lit')
  },

  /** 足迹时间线分页 */
  list(page: number, size: number): Promise<PageResult<CheckinRecordVO>> {
    return request.get('/checkin/list', { page, size })
  },

  /** 软删一条足迹 */
  remove(id: number): Promise<void> {
    return request.delete('/checkin/' + id)
  }
}
