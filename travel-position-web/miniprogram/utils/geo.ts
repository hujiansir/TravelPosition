/**
 * DataV.GeoAtlas 加载、缓存与 GeoJSON→map polygon 转换
 * 坐标体系:DataV 边界为 GCJ02,与小程序 getLocation(type:'gcj02')、腾讯地图一致,无需转换
 * DataV 直连会被小程序 Referer 防盗链拦截(403),改由后端 /map/geojson 代理
 */

import { request } from './request'

const CHINA_ADCODE = 100000
const CACHE_PREFIX = 'geo:v3:'

/** 全国行政区划码 */
export const CHINA = CHINA_ADCODE

/** map 组件 polygon 单元 */
export interface MapPolygon {
  points: { latitude: number; longitude: number }[]
  strokeWidth: number
  strokeColor: string
  fillColor: string
  zIndex: number
}

interface GeoFeature {
  type: string
  properties: { adcode: number; name: string; [key: string]: any }
  geometry: { type: string; coordinates: any } | null
}

interface GeoFeatureCollection {
  type: string
  features: GeoFeature[]
}

// 暗色版图剪影:未点亮区域用深色不透明填充盖住底图道路;已点亮城市用青蓝高亮实心填充
// 注意:微信地图 polygon 颜色须用 8 位十六进制(后两位为 alpha,FF=不透明),6 位会导致填充不显示
const COLOR_LIT = '#00E5FFFF'    // 青蓝高亮实心填充(点亮城市)
const COLOR_UNLIT = '#0E1A2BFF'  // 深蓝黑实心填充(未点亮,盖住底图道路)
const STROKE_LIT = '#7AF5FFFF'   // 浅青描边(点亮城市,模拟发光边)
const STROKE_UNLIT = '#1F3A55FF' // 深蓝描边(省界,暗调中隐约可见)

function cacheKey(adcode: number): string {
  return CACHE_PREFIX + adcode
}

function fetchGeoJson(adcode: number): Promise<GeoFeatureCollection> {
  // 走后端代理(后端拉取 DataV 并缓存),避免小程序直连 DataV 的 403
  return request.get<string>('/map/geojson', { adcode }).then((body) => {
    return typeof body === 'string'
      ? (JSON.parse(body) as GeoFeatureCollection)
      : (body as unknown as GeoFeatureCollection)
  })
}

/**
 * 加载某行政区 GeoJSON(带本地缓存)
 */
export function loadGeoJson(adcode: number): Promise<GeoFeatureCollection> {
  const key = cacheKey(adcode)
  try {
    const cached = wx.getStorageSync(key)
    if (cached) {
      return Promise.resolve(cached as GeoFeatureCollection)
    }
  } catch (e) {
    // storage 读取异常,忽略走网络
  }
  return fetchGeoJson(adcode).then((data) => {
    try {
      wx.setStorageSync(key, data)
    } catch (e) {
      // storage 写入(超限等)异常,忽略
    }
    return data
  })
}

/**
 * 加载全国省级 GeoJSON(版图底图)
 */
export function loadChinaGeoJson(): Promise<GeoFeatureCollection> {
  return loadGeoJson(CHINA_ADCODE)
}

/**
 * 提取几何的外环(只取外环,忽略岛洞)
 */
function extractOuterRings(geometry: GeoFeature['geometry']): number[][][] {
  if (!geometry) {
    return []
  }
  if (geometry.type === 'Polygon') {
    return [geometry.coordinates[0] as number[][]]
  }
  if (geometry.type === 'MultiPolygon') {
    return (geometry.coordinates as any[]).map((poly) => poly[0] as number[][])
  }
  return []
}

/** 直辖市 adcode(省级即市级) */
export const MUNICIPALITIES = new Set<number>([110000, 310000, 120000, 500000])

/**
 * GeoJSON FeatureCollection → map polygons
 * @param fc GeoJSON
 * @param isLit 判定某 adcode 是否点亮
 */
export function geoJsonToPolygons(
  fc: GeoFeatureCollection,
  isLit: (adcode: number) => boolean,
  includeUnlit = true
): MapPolygon[] {
  const polygons: MapPolygon[] = []
  for (const feature of fc.features) {
    const adcode = feature.properties.adcode
    const lit = isLit(adcode)
    // 市级图层:未点亮城市不渲染(includeUnlit=false)
    if (!lit && !includeUnlit) {
      continue
    }
    const rings = extractOuterRings(feature.geometry)
    for (const ring of rings) {
      polygons.push({
        points: ring.map((coord) => ({ latitude: coord[1], longitude: coord[0] })),
        strokeWidth: lit ? 2 : 1,
        strokeColor: lit ? STROKE_LIT : STROKE_UNLIT,
        fillColor: lit ? COLOR_LIT : COLOR_UNLIT,
        zIndex: lit ? 2 : 1
      })
    }
  }
  return polygons
}

export type { GeoFeatureCollection, GeoFeature }
