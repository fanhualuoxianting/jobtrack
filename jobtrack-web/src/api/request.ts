import axios from 'axios'
import type { AxiosResponse } from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // TODO: 阶段2添加 Access Token
    // TODO: 添加 X-Request-ID
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：解包 response.data，调用方直接获得 ApiResponse
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data as unknown as AxiosResponse
  },
  (error) => {
    // TODO: 阶段2实现 Token 刷新队列
    const message = error.response?.data?.message || '网络错误，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export default request
