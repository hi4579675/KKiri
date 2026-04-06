import React, { useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { useGroupStore } from '../../../src/store/groupStore';

export default function GroupListScreen() {
  const { groups, activeGroupId, setActiveGroup } = useGroupStore();

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>그룹</Text>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.closeText}>닫기</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={groups}
        keyExtractor={(g) => String(g.groupId)}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.groupItem}
            onPress={() => {
              setActiveGroup(item.groupId);
              router.replace('/(app)/feed');
            }}
          >
            <View>
              <Text style={styles.groupName}>{item.name}</Text>
              <Text style={styles.groupMeta}>
                최대 {item.maxMembers}명
              </Text>
            </View>
            {activeGroupId === item.groupId && (
              <Text style={styles.activeBadge}>활성</Text>
            )}
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>참여한 그룹이 없어요</Text>
          </View>
        }
        contentContainerStyle={{ paddingBottom: 20 }}
      />

      <View style={styles.footer}>
        <TouchableOpacity
          style={styles.joinButton}
          onPress={() => router.push('/(app)/groups/join')}
        >
          <Text style={styles.joinButtonText}>코드로 참여</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.createButton}
          onPress={() => router.push('/(app)/groups/create')}
        >
          <Text style={styles.createButtonText}>그룹 만들기</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 56,
    paddingBottom: 16,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '700',
  },
  closeText: {
    color: '#8E8E93',
    fontSize: 16,
  },
  groupItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderColor: '#3A3A3C',
  },
  groupName: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
  },
  groupMeta: {
    color: '#8E8E93',
    fontSize: 14,
    marginTop: 2,
  },
  activeBadge: {
    color: '#FFFFFF',
    fontSize: 12,
    backgroundColor: '#2C2C2E',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 9999,
  },
  emptyContainer: {
    alignItems: 'center',
    marginTop: 80,
  },
  emptyText: {
    color: '#8E8E93',
    fontSize: 16,
  },
  footer: {
    flexDirection: 'row',
    gap: 12,
    paddingHorizontal: 16,
    paddingBottom: 40,
    paddingTop: 16,
  },
  joinButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 16,
    backgroundColor: '#1C1C1E',
    alignItems: 'center',
  },
  joinButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  createButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 16,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
  },
  createButtonText: {
    color: '#000000',
    fontWeight: '600',
  },
});
