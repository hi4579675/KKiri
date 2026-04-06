import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Clipboard,
  StyleSheet,
} from 'react-native';
import { router } from 'expo-router';
import { groupApi } from '../../../src/api/group';
import { useGroupStore } from '../../../src/store/groupStore';
import { GroupResponse } from '../../../src/types/api';

export default function CreateGroupScreen() {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState<GroupResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const { addGroup } = useGroupStore();

  const handleCreate = async () => {
    if (!name.trim()) return;
    setLoading(true);
    try {
      const group = await groupApi.createGroup(name.trim());
      addGroup(group);
      setCreated(group);
    } catch {
      Alert.alert('오류', '그룹 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (!created) return;
    Clipboard.setString(created.inviteCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // ── 생성 완료 화면 ──────────────────────────────────
  if (created) {
    return (
      <View style={styles.container}>
        <View style={styles.successHeader}>
          <Text style={styles.successTitle}>그룹이 만들어졌어요!</Text>
        </View>

        {/* 그룹 이름 */}
        <View style={styles.groupNameContainer}>
          <Text style={styles.sectionLabel}>그룹 이름</Text>
          <Text style={styles.groupNameText}>{created.name}</Text>
        </View>

        {/* 초대 코드 */}
        <Text style={styles.sectionLabel}>초대 코드</Text>
        <View style={styles.inviteCodeRow}>
          <Text style={styles.inviteCodeText}>{created.inviteCode}</Text>
          <TouchableOpacity
            onPress={handleCopy}
            style={styles.copyButton}
          >
            <Text style={styles.copyButtonText}>{copied ? '복사됨 ✓' : '복사'}</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.inviteHint}>
          친구에게 이 코드를 공유하면 그룹에 참여할 수 있어요
        </Text>

        {/* 피드로 이동 */}
        <TouchableOpacity
          style={styles.feedButton}
          onPress={() => router.replace('/(app)/feed')}
        >
          <Text style={styles.feedButtonText}>피드로 이동</Text>
        </TouchableOpacity>
      </View>
    );
  }

  // ── 이름 입력 화면 ──────────────────────────────────
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.cancelText}>취소</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>그룹 만들기</Text>
        <View style={{ width: 40 }} />
      </View>

      <Text style={styles.sectionLabel}>그룹 이름</Text>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder="예: 우리들의 하루"
          placeholderTextColor="#8E8E93"
          value={name}
          onChangeText={(t) => setName(t.slice(0, 30))}
          maxLength={30}
          autoFocus
        />
      </View>

      <TouchableOpacity
        style={[styles.createButton, { backgroundColor: name.trim() ? '#FFFFFF' : '#3A3A3C' }]}
        onPress={handleCreate}
        disabled={!name.trim() || loading}
      >
        {loading ? (
          <ActivityIndicator color="#000000" />
        ) : (
          <Text style={[styles.createButtonText, { color: name.trim() ? '#000000' : '#8E8E93' }]}>
            생성
          </Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
    paddingHorizontal: 24,
  },
  successHeader: {
    paddingTop: 56,
    paddingBottom: 32,
  },
  successTitle: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
    textAlign: 'center',
  },
  groupNameContainer: {
    alignItems: 'center',
    marginBottom: 32,
  },
  groupNameText: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '700',
  },
  sectionLabel: {
    color: '#8E8E93',
    fontSize: 12,
    marginBottom: 8,
    letterSpacing: 3,
  },
  inviteCodeRow: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    paddingHorizontal: 20,
    paddingVertical: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  inviteCodeText: {
    color: '#FFFFFF',
    fontSize: 30,
    fontWeight: '700',
    letterSpacing: 3,
  },
  copyButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    backgroundColor: '#2C2C2E',
    borderRadius: 8,
  },
  copyButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
  },
  inviteHint: {
    color: '#8E8E93',
    fontSize: 12,
    marginBottom: 40,
  },
  feedButton: {
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  feedButtonText: {
    fontWeight: '600',
    color: '#000000',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 56,
    paddingBottom: 32,
  },
  cancelText: {
    color: '#8E8E93',
    fontSize: 16,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
  },
  inputContainer: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginBottom: 32,
  },
  input: {
    color: '#FFFFFF',
    fontSize: 16,
  },
  createButton: {
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  createButtonText: {
    fontWeight: '600',
  },
});
