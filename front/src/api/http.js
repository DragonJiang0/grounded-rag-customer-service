import axios from 'axios'

const TOKEN_KEY = 'token'
const LOGIN_PATH = `${import.meta.env.BASE_URL}login`

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/manage',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const url = err.config?.url || ''
    if (status === 401 && !url.includes('/auth/login') && window.location.pathname !== LOGIN_PATH) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.replace(LOGIN_PATH)
    }
    return Promise.reject(err)
  }
)

export function errorMessage(err, fallback = '操作失败，请稍后重试') {
  const data = err?.response?.data
  if (data?.detail) {
    return data.detail
  }
  if (data?.errors) {
    return Object.values(data.errors).join('；')
  }
  if (err?.response?.status) {
    return `${fallback}（HTTP ${err.response.status}）`
  }
  return fallback
}

export default http
