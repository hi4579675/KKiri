import React, { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { tokenExpiredEmitter } from '../src/api/client';
import { useAuthStore } from '../src/store/authStore';
import { router } from 'expo-router';

export default function RootLayout() {
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    const handleTokenExpired = async () => {
      await logout();
      router.replace('/(auth)/login');
    };

    tokenExpiredEmitter.on(handleTokenExpired);
    return () => tokenExpiredEmitter.off(handleTokenExpired);
  }, [logout]);

  return (
    <>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="(auth)" />
        <Stack.Screen name="(app)" />
      </Stack>
    </>
  );
}
