import client from './client';
import { ApiResponse, UserProfile } from '../types/api';

export const userApi = {
  async getMe(): Promise<UserProfile> {
    const res = await client.get<ApiResponse<UserProfile>>('/api/users/me');
    return res.data.data;
  },

  async updateProfile(data: {
    nickname: string;
    avatarEmoji: string;
    avatarColor: string;
  }): Promise<void> {
    await client.patch('/api/users/me/profile', data);
  },

  async checkNickname(nickname: string): Promise<{ available: boolean }> {
    const res = await client.get<ApiResponse<{ available: boolean }>>(
      '/api/users/me/nickname/check',
      { params: { nickname } },
    );
    return res.data.data;
  },

  async completeProfile(data: {
    nickname: string;
    avatarEmoji: string;
    avatarColor: string;
  }): Promise<void> {
    await client.post('/api/users/me/profile', data);
  },

  async updateFcmToken(fcmToken: string): Promise<void> {
    await client.patch('/api/users/me/fcm-token', { fcmToken });
  },

  async updatePushEnabled(pushEnabled: boolean): Promise<void> {
    await client.patch('/api/users/me/push-enabled', { pushEnabled });
  },
};
