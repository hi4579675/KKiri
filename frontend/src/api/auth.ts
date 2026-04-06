import client from './client';
import { TokenResponse, ApiResponse } from '../types/api';

export const authApi = {
  async kakaoLogin(accessToken: string): Promise<TokenResponse> {
    const res = await client.post<ApiResponse<TokenResponse>>('/api/auth/kakao', { accessToken });
    return res.data.data;
  },

  async kakaoLoginWithCode(code: string, redirectUri: string): Promise<TokenResponse> {
    const res = await client.post<ApiResponse<TokenResponse>>('/api/auth/kakao/code', { code, redirectUri });
    return res.data.data;
  },

  async refresh(refreshToken: string): Promise<TokenResponse> {
    const res = await client.post<ApiResponse<TokenResponse>>('/api/auth/refresh', { refreshToken });
    return res.data.data;
  },
};
