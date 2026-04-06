import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  Dimensions,
  Animated,
  FlatList,
  ViewToken,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useFeedStore } from '../../../src/store/feedStore';
import { Avatar } from '../../../src/components/common/Avatar';
import { ContributorResponse, PostResponse } from '../../../src/types/api';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');
const STORY_DURATION = 3000;

interface StoryItem {
  member: ContributorResponse;
  post: PostResponse;
  memberPostCount: number;
  memberPostIdx: number;
}

export default function StoryScreen() {
  const { groupId, userId } = useLocalSearchParams<{ groupId: string; userId?: string }>();
  const buckets = useFeedStore((s) => s.buckets);
  const contributors = useFeedStore((s) => s.contributors);

  const allPosts = buckets.flatMap((b) => b.posts);

  // 모든 스토리를 플랫 배열로
  const items: StoryItem[] = contributors.flatMap((c) => {
    const posts = allPosts.filter((p) => p.userId === c.userId);
    return posts.map((post, idx) => ({
      member: c,
      post,
      memberPostCount: posts.length,
      memberPostIdx: idx,
    }));
  }).filter((item) => item.memberPostCount > 0);

  const initialIdx = userId
    ? Math.max(0, items.findIndex((item) => item.member.userId === Number(userId)))
    : 0;

  const [currentIdx, setCurrentIdx] = useState(initialIdx);
  const flatListRef = useRef<FlatList>(null);
  const progress = useRef(new Animated.Value(0)).current;
  const animationRef = useRef<Animated.CompositeAnimation | null>(null);

  const currentItem = items[currentIdx];

  const goNext = useCallback(() => {
    if (currentIdx < items.length - 1) {
      flatListRef.current?.scrollToIndex({ index: currentIdx + 1, animated: true });
    } else {
      router.back();
    }
  }, [currentIdx, items.length]);

  const goPrev = useCallback(() => {
    if (currentIdx > 0) {
      flatListRef.current?.scrollToIndex({ index: currentIdx - 1, animated: true });
    }
  }, [currentIdx]);

  // 자동 진행 타이머
  useEffect(() => {
    progress.setValue(0);
    animationRef.current?.stop();

    animationRef.current = Animated.timing(progress, {
      toValue: 1,
      duration: STORY_DURATION,
      useNativeDriver: false,
    });
    animationRef.current.start(({ finished }) => {
      if (finished) goNext();
    });

    return () => animationRef.current?.stop();
  }, [currentIdx]);

  // 스크롤로 슬라이드 변경 시
  const onViewableItemsChanged = useRef(({ viewableItems }: { viewableItems: ViewToken[] }) => {
    if (viewableItems.length > 0 && viewableItems[0].index !== null) {
      setCurrentIdx(viewableItems[0].index);
    }
  }).current;

  const viewabilityConfig = useRef({ itemVisiblePercentThreshold: 60 }).current;

  if (!currentItem) return null;

  const formattedTime = () => {
    const date = new Date(currentItem.post.createdAt);
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    return `${h}:${m}`;
  };

  return (
    <View style={{ flex: 1, backgroundColor: '#000000' }}>
      <FlatList
        ref={flatListRef}
        data={items}
        keyExtractor={(item, i) => `${item.post.postId}-${i}`}
        pagingEnabled
        showsVerticalScrollIndicator={false}
        initialScrollIndex={initialIdx}
        getItemLayout={(_, index) => ({
          length: SCREEN_HEIGHT,
          offset: SCREEN_HEIGHT * index,
          index,
        })}
        onViewableItemsChanged={onViewableItemsChanged}
        viewabilityConfig={viewabilityConfig}
        renderItem={({ item }) => (
          <View style={{ width: SCREEN_WIDTH, height: SCREEN_HEIGHT }}>
            {/* 배경: 검정 + 이미지 가로 기준 contain */}
            <View style={{ position: 'absolute', width: SCREEN_WIDTH, height: SCREEN_HEIGHT, backgroundColor: '#000000', alignItems: 'center', justifyContent: 'center' }}>
              <Image
                source={{ uri: item.post.imageUrl }}
                style={{ width: SCREEN_WIDTH, height: SCREEN_HEIGHT }}
                resizeMode="contain"
              />
            </View>

            {/* 탭: 좌(이전) / 우(다음) */}
            <View style={{ position: 'absolute', flexDirection: 'row', width: SCREEN_WIDTH, height: SCREEN_HEIGHT }}>
              <TouchableOpacity style={{ flex: 1 }} onPress={goPrev} activeOpacity={1} />
              <TouchableOpacity style={{ flex: 1 }} onPress={goNext} activeOpacity={1} />
            </View>

            {/* 상단: 진행 바 + 아바타 + 닫기 */}
            <View style={{ paddingTop: 56, paddingHorizontal: 12 }}>
              {/* 이 멤버의 포스트 진행 바 */}
              <View style={{ flexDirection: 'row', gap: 4, marginBottom: 12 }}>
                {Array.from({ length: item.memberPostCount }).map((_, i) => (
                  <View
                    key={i}
                    style={{ flex: 1, height: 3, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.3)' }}
                  >
                    {i === item.memberPostIdx && item.post.postId === currentItem.post.postId && (
                      <Animated.View
                        style={{
                          height: 3,
                          borderRadius: 2,
                          backgroundColor: '#FFFFFF',
                          width: progress.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }),
                        }}
                      />
                    )}
                    {i < item.memberPostIdx && (
                      <View style={{ height: 3, borderRadius: 2, backgroundColor: '#FFFFFF' }} />
                    )}
                  </View>
                ))}
              </View>

              {/* 아바타 + 이름 + 닫기 */}
              <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
                  <Avatar emoji={item.member.avatarEmoji} color={item.member.avatarColor} size={36} borderWidth={2} />
                  <Text style={{ color: '#FFFFFF', fontWeight: '600', fontSize: 15 }}>
                    {item.member.nickname}
                  </Text>
                </View>
                <TouchableOpacity
                  onPress={() => router.back()}
                  style={{
                    width: 36, height: 36, borderRadius: 18,
                    backgroundColor: 'rgba(60,60,60,0.7)',
                    alignItems: 'center', justifyContent: 'center',
                  }}
                >
                  <Ionicons name="close" size={20} color="#FFFFFF" />
                </TouchableOpacity>
              </View>
            </View>

            {/* 하단: 시간 + 캡션 + 반응 */}
            <View
              style={{
                position: 'absolute', bottom: 0, left: 0, right: 0,
                paddingHorizontal: 20, paddingBottom: 48,
              }}
            >
              <Text style={{ color: '#FFFFFF', fontSize: 36, fontWeight: '800', lineHeight: 42 }}>
                {(() => {
                  const d = new Date(item.post.createdAt);
                  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                })()}
              </Text>
              {item.post.caption && (
                <Text style={{ color: '#FFFFFF', fontSize: 16, marginTop: 4, opacity: 0.9 }}>
                  {item.post.caption}
                </Text>
              )}
              <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 20, gap: 12 }}>
                <TouchableOpacity
                  style={{
                    width: 44, height: 44, borderRadius: 22,
                    backgroundColor: 'rgba(60,60,60,0.7)',
                    alignItems: 'center', justifyContent: 'center',
                  }}
                >
                  <Text style={{ fontSize: 22 }}>♡</Text>
                </TouchableOpacity>
                <View
                  style={{
                    flex: 1, height: 44, borderRadius: 22,
                    backgroundColor: 'rgba(60,60,60,0.7)',
                    justifyContent: 'center', paddingHorizontal: 16,
                  }}
                >
                  <Text style={{ color: '#8E8E93', fontSize: 14 }}>댓글 달기...</Text>
                </View>
              </View>
            </View>
          </View>
        )}
      />
    </View>
  );
}
