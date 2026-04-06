import { create } from 'zustand';
import { tokenStorage } from '../utils/tokenStorage';

interface UserProfile {
  nickname: string;
  avatarEmoji: string;
  avatarColor: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  profileCompleted: boolean;
  isAuthenticated: boolean;
  userProfile: UserProfile | null;

  setTokens: (access: string, refresh: string, profileCompleted: boolean) => Promise<void>;
  setProfileCompleted: (v: boolean) => void;
  setUserProfile: (profile: UserProfile) => void;
  logout: () => Promise<void>;
  hydrate: () => Promise<{ isAuthenticated: boolean; profileCompleted: boolean }>;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  profileCompleted: false,
  isAuthenticated: false,
  userProfile: null,

  setTokens: async (access, refresh, profileCompleted) => {
    await tokenStorage.saveTokens(access, refresh);
    set({ accessToken: access, refreshToken: refresh, profileCompleted, isAuthenticated: true });
  },

  setProfileCompleted: (v) => set({ profileCompleted: v }),

  setUserProfile: (profile) => set({ userProfile: profile }),

  logout: async () => {
    await tokenStorage.clearTokens();
    set({ accessToken: null, refreshToken: null, profileCompleted: false, isAuthenticated: false, userProfile: null });
  },

  hydrate: async () => {
    const accessToken = await tokenStorage.getAccessToken();
    const refreshToken = await tokenStorage.getRefreshToken();
    if (accessToken && refreshToken) {
      // profileCompleted는 서버에서 다시 확인하거나 토큰 디코딩으로 알 수 있지만
      // 간단하게 로컬에 저장해둔 값이 없으면 true로 가정 (프로필 완료 후 접근)
      set({ accessToken, refreshToken, isAuthenticated: true, profileCompleted: true });
      return { isAuthenticated: true, profileCompleted: true };
    }
    return { isAuthenticated: false, profileCompleted: false };
  },
}));
