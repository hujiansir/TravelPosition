import { config } from '../config/index'

/**
 * HTTP 请求方法类型
 */
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

/**
 * 请求配置接口
 */
interface RequestOptions {
  url: string
  method?: HttpMethod
  data?: any
  header?: any
  showLoading?: boolean
  loadingText?: string
}

/**
 * 响应结果接口
 */
interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * HTTP 请求类
 */
class Request {
  private baseUrl: string
  private timeout: number

  constructor() {
    this.baseUrl = config.baseUrl
    this.timeout = config.timeout
  }

  /**
   * 发起请求
   */
  request<T = any>(options: RequestOptions): Promise<T> {
    const {
      url,
      method = 'GET',
      data,
      header = {},
      showLoading = config.showLoading,
      loadingText = config.loadingText
    } = options

    // 显示加载提示
    if (showLoading) {
      wx.showLoading({
        title: loadingText,
        mask: true
      })
    }

    return new Promise((resolve, reject) => {
      wx.request({
        url: this.baseUrl + url,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          ...header
        },
        timeout: this.timeout,
        success: (res) => {
          if (showLoading) {
            wx.hideLoading()
          }

          const result = res.data as ResponseData<T>

          // 请求成功
          if (result.code === 200) {
            resolve(result.data)
          } else {
            // 业务错误
            wx.showToast({
              title: result.message || '请求失败',
              icon: 'none',
              duration: 2000
            })
            reject(new Error(result.message))
          }
        },
        fail: (err) => {
          if (showLoading) {
            wx.hideLoading()
          }

          wx.showToast({
            title: '网络请求失败',
            icon: 'none',
            duration: 2000
          })
          reject(err)
        }
      })
    })
  }

  /**
   * GET 请求
   */
  get<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
    return this.request<T>({
      url,
      method: 'GET',
      data,
      ...options
    })
  }

  /**
   * POST 请求
   */
  post<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
    return this.request<T>({
      url,
      method: 'POST',
      data,
      ...options
    })
  }

  /**
   * PUT 请求
   */
  put<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
    return this.request<T>({
      url,
      method: 'PUT',
      data,
      ...options
    })
  }

  /**
   * DELETE 请求
   */
  delete<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
    return this.request<T>({
      url,
      method: 'DELETE',
      data,
      ...options
    })
  }
}

// 导出请求实例
export const request = new Request()
