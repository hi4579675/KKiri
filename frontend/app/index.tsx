import React, { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { useAuthStore } from '../src/store/authStore';
import { useGroupStore } from '../src/store/groupStore';
import { groupApi } from '../src/api/group';

export default function SplashScreen() {
  const hydrate = useAuthStore((s) => s.hydrate);
  const setGroups = useGroupStore((s) => s.setGroups);

  useEffect(() => {
    hydrate().then(async ({ isAuthenticated, profileCompleted }) => {
      if (!isAuthenticated) {
        router.replace('/(auth)/login');
      } else if (!profileCompleted) {
        router.replace('/(auth)/profile-setup');
      } else {
        // 내 그룹 불러오기
        try {
          const groups = await groupApi.getMyGroups();
          setGroups(groups);
        } catch (e: any) {
          // 403 = 프로필 미완료 (DB 불일치) → 프로필 설정으로
          if (e?.response?.status === 403) {
            router.replace('/(auth)/profile-setup');
            return;
          }
          // 그 외 오류는 빈 피드로 이동
        }
        router.replace('/(app)/feed');
      }
    });
  }, []);

  return (
    <View style={styles.container}>
      <ActivityIndicator color="#FFFFFF" size="large" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#000000',
  },
});
