import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Share,
  StyleSheet,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { groupApi } from '../../../src/api/group';
import { useGroupStore } from '../../../src/store/groupStore';
import { GroupMember } from '../../../src/types/api';
import { Avatar } from '../../../src/components/common/Avatar';
import { ConfirmDialog } from '../../../src/components/common/ConfirmDialog';

export default function GroupDetailScreen() {
  const { groupId } = useLocalSearchParams<{ groupId: string }>();
  const gid = Number(groupId);

  const { groups, members, setMembers, updateGroup, removeGroup } = useGroupStore();
  const group = groups.find((g) => g.groupId === gid);
  const memberList = members[gid] ?? [];

  const [loading, setLoading] = useState(false);
  const [kickTarget, setKickTarget] = useState<GroupMember | null>(null);
  const [transferTarget, setTransferTarget] = useState<GroupMember | null>(null);
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);

  // 현재 사용자 (임시: 첫 번째 OWNER)
  const myMember = memberList.find((m) => m.role === 'OWNER');
  const isOwner = myMember?.role === 'OWNER';

  useEffect(() => {
    if (!gid) return;
    groupApi.getMembers(gid).then((list) => setMembers(gid, list)).catch(() => {});
    groupApi.getInviteCode(gid).then((res) => {
      updateGroup(gid, { inviteCode: res.inviteCode, inviteCodeExpiredAt: res.inviteCodeExpiredAt });
    }).catch(() => {});
  }, [gid]);

  const isExpired = group ? new Date() > new Date(group.inviteCodeExpiredAt) : false;

  const handleRenewCode = async () => {
    if (!group) return;
    try {
      const res = await groupApi.renewInviteCode(gid);
      updateGroup(gid, { inviteCode: res.inviteCode, inviteCodeExpiredAt: res.inviteCodeExpiredAt });
    } catch {
      Alert.alert('오류', '코드 갱신에 실패했습니다.');
    }
  };

  const handleShare = () => {
    if (!group) return;
    Share.share({ message: `끼리 초대 코드: ${group.inviteCode}` });
  };

  const handleKick = async () => {
    if (!kickTarget) return;
    try {
      await groupApi.kickMember(gid, kickTarget.userId);
      setMembers(gid, memberList.filter((m) => m.userId !== kickTarget.userId));
    } catch {
      Alert.alert('오류', '강퇴에 실패했습니다.');
    }
    setKickTarget(null);
  };

  const handleTransfer = async () => {
    if (!transferTarget) return;
    try {
      await groupApi.transferOwner(gid, transferTarget.userId);
      setMembers(
        gid,
        memberList.map((m) => ({
          ...m,
          role: m.userId === transferTarget.userId ? 'OWNER' : 'MEMBER',
        })),
      );
    } catch {
      Alert.alert('오류', '방장 위임에 실패했습니다.');
    }
    setTransferTarget(null);
  };

  const handleLeave = async () => {
    try {
      await groupApi.leaveGroup(gid);
      removeGroup(gid);
      router.replace('/(app)/feed');
    } catch {
      Alert.alert('오류', '그룹 탈퇴에 실패했습니다.');
    }
    setShowLeaveDialog(false);
  };

  if (!group) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator color="#8E8E93" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backText}>뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{group.name}</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* 초대 코드 카드 */}
      <View style={styles.inviteCard}>
        <Text style={styles.inviteLabel}>초대 코드</Text>
        <View style={styles.inviteRow}>
          <Text
            style={[styles.inviteCode, { opacity: isExpired ? 0.4 : 1 }]}
          >
            {group.inviteCode}
          </Text>
          {isExpired ? (
            isOwner && (
              <TouchableOpacity
                style={styles.codeActionButton}
                onPress={handleRenewCode}
              >
                <Text style={styles.codeActionText}>갱신</Text>
              </TouchableOpacity>
            )
          ) : (
            <TouchableOpacity
              style={styles.codeActionButton}
              onPress={handleShare}
            >
              <Text style={styles.codeActionText}>공유</Text>
            </TouchableOpacity>
          )}
        </View>
        {isExpired && (
          <Text style={styles.expiredText}>만료됨</Text>
        )}
        {!isExpired && (
          <Text style={styles.expiryText}>
            만료: {new Date(group.inviteCodeExpiredAt).toLocaleDateString('ko-KR')}
          </Text>
        )}
      </View>

      {/* 멤버 목록 */}
      <Text style={styles.memberCountLabel}>
        멤버 {memberList.length}/{group.maxMembers}
      </Text>
      {memberList.map((member) => (
        <View
          key={member.userId}
          style={styles.memberRow}
        >
          <Avatar emoji={member.avatarEmoji} color={member.avatarColor} size={40} />
          <View style={styles.memberInfo}>
            <Text style={styles.memberName}>{member.nickname}</Text>
            {member.role === 'OWNER' && (
              <Text style={styles.ownerLabel}>방장</Text>
            )}
          </View>
          {isOwner && member.role !== 'OWNER' && (
            <View style={styles.memberActions}>
              <TouchableOpacity
                style={styles.transferButton}
                onPress={() => setTransferTarget(member)}
              >
                <Text style={styles.actionText}>위임</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.kickButton}
                onPress={() => setKickTarget(member)}
              >
                <Text style={styles.kickText}>강퇴</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      ))}

      {/* 탈퇴 버튼 */}
      <TouchableOpacity
        style={styles.leaveButton}
        onPress={() => setShowLeaveDialog(true)}
      >
        <Text style={styles.leaveButtonText}>그룹 나가기</Text>
      </TouchableOpacity>

      {/* 다이얼로그들 */}
      <ConfirmDialog
        visible={!!kickTarget}
        title={`${kickTarget?.nickname}님을 강퇴할까요?`}
        confirmLabel="강퇴"
        destructive
        onConfirm={handleKick}
        onCancel={() => setKickTarget(null)}
      />
      <ConfirmDialog
        visible={!!transferTarget}
        title={`${transferTarget?.nickname}님에게 방장을 위임할까요?`}
        confirmLabel="위임"
        onConfirm={handleTransfer}
        onCancel={() => setTransferTarget(null)}
      />
      <ConfirmDialog
        visible={showLeaveDialog}
        title="그룹을 나갈까요?"
        message="나가면 오늘의 추억도 함께 사라져요"
        confirmLabel="나가기"
        destructive
        onConfirm={handleLeave}
        onCancel={() => setShowLeaveDialog(false)}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    backgroundColor: '#000000',
    alignItems: 'center',
    justifyContent: 'center',
  },
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
  backText: {
    color: '#8E8E93',
    fontSize: 16,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
  },
  inviteCard: {
    marginHorizontal: 16,
    backgroundColor: '#1C1C1E',
    borderRadius: 16,
    padding: 20,
    marginBottom: 24,
  },
  inviteLabel: {
    color: '#8E8E93',
    fontSize: 12,
    marginBottom: 8,
  },
  inviteRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  inviteCode: {
    color: '#FFFFFF',
    fontSize: 30,
    fontWeight: '700',
    letterSpacing: 3,
  },
  codeActionButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: '#2C2C2E',
    borderRadius: 8,
  },
  codeActionText: {
    color: '#FFFFFF',
    fontSize: 14,
  },
  expiredText: {
    color: '#FF3B30',
    fontSize: 12,
    marginTop: 8,
  },
  expiryText: {
    color: '#8E8E93',
    fontSize: 12,
    marginTop: 8,
  },
  memberCountLabel: {
    color: '#8E8E93',
    fontSize: 12,
    paddingHorizontal: 16,
    marginBottom: 12,
    letterSpacing: 3,
  },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderColor: '#3A3A3C',
  },
  memberInfo: {
    flex: 1,
    marginLeft: 12,
  },
  memberName: {
    color: '#FFFFFF',
    fontWeight: '500',
  },
  ownerLabel: {
    color: '#8E8E93',
    fontSize: 12,
  },
  memberActions: {
    flexDirection: 'row',
    gap: 8,
  },
  transferButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    backgroundColor: '#2C2C2E',
    borderRadius: 8,
  },
  kickButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: '#FF3B3020',
  },
  actionText: {
    color: '#FFFFFF',
    fontSize: 12,
  },
  kickText: {
    color: '#FF3B30',
    fontSize: 12,
  },
  leaveButton: {
    marginHorizontal: 16,
    marginTop: 32,
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
    backgroundColor: '#FF3B3015',
  },
  leaveButtonText: {
    color: '#FF3B30',
    fontWeight: '600',
  },
});
