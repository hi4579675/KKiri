import { useEffect } from 'react';
import { View } from 'react-native';
import { router } from 'expo-router';
import { useAuthStore } from '../src/store/authStore';

export default function NotFound() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    router.replace(isAuthenticated ? '/(app)/feed' : '/(auth)/login');
  }, []);

  return <View style={{ flex: 1, backgroundColor: '#000000' }} />;
}
