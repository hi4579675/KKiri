import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../constants/config';
import { tokenStorage } from '../utils/tokenStorage';
import { TokenResponse, ApiResponse } from '../types/api';

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

// 동시 401 대응: refresh 중인 Promise 보관
let refreshingPromise: Promise<string> | null = null;

client.interceptors.request.use(async (config) => {
  const token = await tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    console.log(`[API] ${response.config.method?.toUpperCase()} ${response.config.url} → ${response.status}`);
    return response;
  },
  async (error: AxiosError) => {
    console.error(`[API ERROR] ${error.config?.method?.toUpperCase()} ${error.config?.url} → ${error.response?.status}`, error.response?.data);
    const originalConfig = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalConfig._retry) {
      originalConfig._retry = true;

      try {
        if (!refreshingPromise) {
          refreshingPromise = (async () => {
            const refreshToken = await tokenStorage.getRefreshToken();
            if (!refreshToken) throw new Error('no refresh token');

            const res = await axios.post<ApiResponse<TokenResponse>>(
              `${API_BASE_URL}/api/auth/refresh`,
              { refreshToken },
            );
            const { accessToken, refreshToken: newRefresh } = res.data.data;
            await tokenStorage.saveTokens(accessToken, newRefresh);
            return accessToken;
          })().finally(() => {
            refreshingPromise = null;
          });
        }

        const newAccessToken = await refreshingPromise;
        originalConfig.headers.Authorization = `Bearer ${newAccessToken}`;
        return client(originalConfig);
      } catch {
        await tokenStorage.clearTokens();
        // 라우터 접근을 위해 이벤트 발행 (authStore에서 구독)
        tokenExpiredEmitter.emit();
      }
    }

    return Promise.reject(error);
  },
);

// 토큰 만료 이벤트 (authStore → 라우터 리다이렉트 용)
type Listener = () => void;
const tokenExpiredEmitter = {
  listeners: [] as Listener[],
  on(fn: Listener) { this.listeners.push(fn); },
  off(fn: Listener) { this.listeners = this.listeners.filter(l => l !== fn); },
  emit() { this.listeners.forEach(l => l()); },
};

export { tokenExpiredEmitter };
export default client;
