import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const KEYS = {
  ACCESS_TOKEN: 'kkiri_access_token',
  REFRESH_TOKEN: 'kkiri_refresh_token',
};

// 웹에서는 SecureStore 미지원 → localStorage 폴백
const storage = {
  async set(key: string, value: string) {
    if (Platform.OS === 'web') {
      localStorage.setItem(key, value);
    } else {
      await SecureStore.setItemAsync(key, value);
    }
  },
  async get(key: string): Promise<string | null> {
    if (Platform.OS === 'web') {
      return localStorage.getItem(key);
    }
    return SecureStore.getItemAsync(key);
  },
  async remove(key: string) {
    if (Platform.OS === 'web') {
      localStorage.removeItem(key);
    } else {
      await SecureStore.deleteItemAsync(key);
    }
  },
};

export const tokenStorage = {
  async saveTokens(accessToken: string, refreshToken: string) {
    await storage.set(KEYS.ACCESS_TOKEN, accessToken);
    await storage.set(KEYS.REFRESH_TOKEN, refreshToken);
  },

  async getAccessToken(): Promise<string | null> {
    return storage.get(KEYS.ACCESS_TOKEN);
  },

  async getRefreshToken(): Promise<string | null> {
    return storage.get(KEYS.REFRESH_TOKEN);
  },

  async clearTokens() {
    await storage.remove(KEYS.ACCESS_TOKEN);
    await storage.remove(KEYS.REFRESH_TOKEN);
  },
};
