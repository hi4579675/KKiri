import React from 'react';
import { Tabs } from 'expo-router';
import { View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export default function AppLayout() {
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#000000',
          borderTopColor: '#3A3A3C',
          borderTopWidth: 0.5,
          height: 56 + insets.bottom,
          paddingBottom: insets.bottom,
        },
        tabBarActiveTintColor: '#FFFFFF',
        tabBarInactiveTintColor: '#8E8E93',
        tabBarShowLabel: false,
      }}
    >
      <Tabs.Screen
        name="feed/index"
        options={{
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="home" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="feed/upload"
        options={{
          tabBarIcon: () => (
            <View
              style={{
                width: 48,
                height: 48,
                borderRadius: 24,
                backgroundColor: '#FFFFFF',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: insets.bottom > 0 ? 8 : 4,
              }}
            >
              <Ionicons name="add" size={28} color="#000000" />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="archive/index"
        options={{
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="albums-outline" size={size} color={color} />
          ),
        }}
      />
      {/* 숨겨진 탭들 */}
      <Tabs.Screen name="archive/[date]" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="groups/index" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="groups/create" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="groups/join" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="groups/[groupId]" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="story/[groupId]" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
      <Tabs.Screen name="settings/index" options={{ tabBarButton: () => null, tabBarItemStyle: { display: 'none' } }} />
    </Tabs>
  );
}
