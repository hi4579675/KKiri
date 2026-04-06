import React, { useCallback, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  RefreshControl,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { router } from 'expo-router';
import { useFeedStore } from '../../../src/store/feedStore';
import { useGroupStore } from '../../../src/store/groupStore';
import { useAuthStore } from '../../../src/store/authStore';
import { postApi } from '../../../src/api/post';
import { userApi } from '../../../src/api/user';
import { HourBucketSection } from '../../../src/components/feed/HourBucketSection';
import { MemberAvatarRow } from '../../../src/components/feed/MemberAvatarRow';
import { Avatar } from '../../../src/components/common/Avatar';
import { ContributorResponse } from '../../../src/types/api';

export default function FeedScreen() {
  const { buckets, contributors, isLoading, setFeedResponse, setLoading } = useFeedStore();
  const { activeGroupId, groups } = useGroupStore();
  const { userProfile, setUserProfile } = useAuthStore();

  const activeGroup = groups.find((g) => g.groupId === activeGroupId);

  useEffect(() => {
    if (!userProfile) {
      userApi.getMe().then((p) => setUserProfile(p)).catch(() => {});
    }
  }, []);

  const loadFeed = useCallback(async () => {
    if (!activeGroupId) return;
    setLoading(true);
    try {
      const feed = await postApi.getFeed(activeGroupId);
      setFeedResponse(feed);
    } catch {
      // 피드 없는 초기 상태도 허용
    } finally {
      setLoading(false);
    }
  }, [activeGroupId]);

  useEffect(() => {
    loadFeed();
  }, [loadFeed]);

  const handleMemberPress = (contributor: ContributorResponse) => {
    router.push(`/(app)/story/${activeGroupId}?userId=${contributor.userId}`);
  };

  if (!activeGroupId) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyTitle}>끼리</Text>
        <Text style={styles.emptySubtitle}>
          그룹을 만들거나 초대 코드로 참여해보세요
        </Text>
        <TouchableOpacity
          style={styles.createButton}
          onPress={() => router.push('/(app)/groups/create')}
        >
          <Text style={styles.createButtonText}>그룹 만들기</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.joinButton}
          onPress={() => router.push('/(app)/groups/join')}
        >
          <Text style={styles.joinButtonText}>그룹 들어가기</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.push('/(app)/groups/index')}>
          <Text style={styles.headerTitle}>
            {activeGroup?.name ?? '오늘'}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => router.push('/(app)/settings')}>
          {userProfile ? (
            <Avatar emoji={userProfile.avatarEmoji} color={userProfile.avatarColor} size={32} borderWidth={2} />
          ) : (
            <View style={styles.profilePlaceholder} />
          )}
        </TouchableOpacity>
      </View>

      {/* 멤버 아바타 줄 */}
      <View style={{ paddingTop: 4 }}>
        <MemberAvatarRow contributors={contributors} onPressMember={handleMemberPress} />
      </View>

      {/* 피드 */}
      {isLoading ? (
        <View style={styles.centered}>
          <ActivityIndicator color="#FFFFFF" />
        </View>
      ) : buckets.length === 0 ? (
        <View style={styles.centered}>
          <Text style={styles.mutedBase}>아직 오늘 포스트가 없어요</Text>
          <Text style={styles.mutedSmall}>첫 번째로 올려보세요 📸</Text>
        </View>
      ) : (
        <FlatList
          data={buckets}
          keyExtractor={(item) => String(item.hour)}
          renderItem={({ item }) => <HourBucketSection bucket={item} />}
          refreshControl={
            <RefreshControl refreshing={isLoading} onRefresh={loadFeed} tintColor="#FFFFFF" />
          }
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: 20 }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#000000',
    paddingHorizontal: 32,
  },
  emptyTitle: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  emptySubtitle: {
    color: '#8E8E93',
    fontSize: 14,
    marginBottom: 40,
    textAlign: 'center',
  },
  createButton: {
    width: '100%',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    marginBottom: 12,
  },
  createButtonText: {
    color: '#000000',
    fontWeight: '600',
    fontSize: 16,
  },
  joinButton: {
    width: '100%',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
    backgroundColor: '#1C1C1E',
  },
  joinButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 56,
    paddingBottom: 12,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
  },
  profilePlaceholder: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#3A3A3C',
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  mutedBase: {
    color: '#8E8E93',
    fontSize: 16,
  },
  mutedSmall: {
    color: '#8E8E93',
    fontSize: 14,
    marginTop: 4,
  },
});
