import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, ActivityIndicator, Alert, Platform, StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { authApi } from '../../src/api/auth';
import { useAuthStore } from '../../src/store/authStore';
import { useGroupStore } from '../../src/store/groupStore';
import { groupApi } from '../../src/api/group';

WebBrowser.maybeCompleteAuthSession();

const KAKAO_REST_API_KEY = '78038827dd441743e2bd48c417e4a8df';
const WEB_REDIRECT_URI = 'http://localhost:8081';

export default function LoginScreen() {
  const [loading, setLoading] = useState(false);
  const setTokens = useAuthStore((s) => s.setTokens);
  const setGroups = useGroupStore((s) => s.setGroups);
  const params = useLocalSearchParams<{ code?: string }>();

  // 웹: 카카오가 code 파라미터로 리다이렉트하는 경우 처리
  useEffect(() => {
    if (Platform.OS !== 'web') return;
    const code = params.code ?? new URLSearchParams(window.location.search).get('code');
    if (code) handleAuthCode(code);
  }, [params.code]);

  const handleAuthCode = async (code: string) => {
    setLoading(true);
    try {
      const { accessToken, refreshToken, profileCompleted } =
        await authApi.kakaoLoginWithCode(code, WEB_REDIRECT_URI);
      await finishLogin(accessToken, refreshToken, profileCompleted);
    } catch (e: any) {
      Alert.alert('로그인 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const finishLogin = async (accessToken: string, refreshToken: string, profileCompleted: boolean) => {
    await setTokens(accessToken, refreshToken, profileCompleted);
    if (profileCompleted) {
      try {
        const groups = await groupApi.getMyGroups();
        setGroups(groups);
      } catch {}
    }
    router.replace(profileCompleted ? '/(app)/feed' : '/(auth)/profile-setup');
  };

  const handleKakaoLogin = async () => {
    if (Platform.OS === 'web') {
      const authUrl =
        `https://kauth.kakao.com/oauth/authorize` +
        `?client_id=${KAKAO_REST_API_KEY}` +
        `&redirect_uri=${encodeURIComponent(WEB_REDIRECT_URI)}` +
        `&response_type=code`;
      window.location.href = authUrl;
      return;
    }

    // 네이티브: Kakao native SDK 사용 (KakaoTalk 또는 카카오계정 브라우저 로그인)
    setLoading(true);
    try {
      const { login } = await import('@react-native-seoul/kakao-login');
      const { accessToken: kakaoToken } = await login();
      console.log('[Login] Kakao native login success');

      const { accessToken, refreshToken, profileCompleted } =
        await authApi.kakaoLogin(kakaoToken);
      await finishLogin(accessToken, refreshToken, profileCompleted);
    } catch (e: any) {
      if (e?.code !== 'E_CANCELLED') {
        Alert.alert('로그인 실패', e?.message ?? '다시 시도해주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>끼리</Text>
      <Text style={styles.subtitle}>우리끼리 공유하는 하루</Text>

      <TouchableOpacity
        style={[styles.kakaoButton, { backgroundColor: '#FEE500' }]}
        onPress={handleKakaoLogin}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#000000" />
        ) : (
          <Text style={styles.kakaoButtonText}>카카오로 계속하기</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#000000',
    paddingHorizontal: 32,
  },
  title: {
    color: '#FFFFFF',
    fontSize: 36,
    fontWeight: '700',
    marginBottom: 8,
  },
  subtitle: {
    color: '#8E8E93',
    fontSize: 16,
    marginBottom: 64,
  },
  kakaoButton: {
    width: '100%',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  kakaoButtonText: {
    color: '#000000',
    fontWeight: '600',
    fontSize: 16,
  },
});
