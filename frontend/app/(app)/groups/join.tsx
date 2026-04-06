import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, Alert, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { groupApi } from '../../../src/api/group';
import { useGroupStore } from '../../../src/store/groupStore';

export default function JoinGroupScreen() {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const { addGroup, setActiveGroup } = useGroupStore();

  const handleJoin = async () => {
    if (code.length !== 6) return;
    setLoading(true);
    try {
      const result = await groupApi.joinGroup(code.toUpperCase());
      addGroup({
        groupId: result.groupId,
        name: result.name,
        inviteCode: code.toUpperCase(),
        inviteCodeExpiredAt: '',
        maxMembers: 6,
      });
      setActiveGroup(result.groupId);
      router.replace('/(app)/feed');
    } catch (e: any) {
      const code = e?.response?.data?.code;
      const messages: Record<string, string> = {
        GROUP_NOT_FOUND: '존재하지 않는 코드예요',
        GROUP_FULL: '그룹이 가득 찼어요',
        INVITE_CODE_EXPIRED: '만료된 코드예요',
        ALREADY_MEMBER: '이미 참여 중인 그룹이에요',
      };
      Alert.alert('참여 실패', messages[code] ?? '다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.cancelText}>취소</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>코드로 참여</Text>
        <View style={{ width: 40 }} />
      </View>

      <Text style={styles.sectionLabel}>초대 코드</Text>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.codeInput}
          placeholder="6자리 코드"
          placeholderTextColor="#8E8E93"
          value={code}
          onChangeText={(t) => setCode(t.toUpperCase().slice(0, 6))}
          maxLength={6}
          autoCapitalize="characters"
          autoFocus
          autoCorrect={false}
        />
      </View>

      <TouchableOpacity
        style={[styles.joinButton, { backgroundColor: code.length === 6 ? '#FFFFFF' : '#3A3A3C' }]}
        onPress={handleJoin}
        disabled={code.length !== 6 || loading}
      >
        {loading ? (
          <ActivityIndicator color="#000000" />
        ) : (
          <Text
            style={[styles.joinButtonText, { color: code.length === 6 ? '#000000' : '#8E8E93' }]}
          >
            참여하기
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
  sectionLabel: {
    color: '#8E8E93',
    fontSize: 12,
    marginBottom: 8,
    letterSpacing: 3,
  },
  inputContainer: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginBottom: 32,
  },
  codeInput: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '700',
    letterSpacing: 3,
    textAlign: 'center',
  },
  joinButton: {
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  joinButtonText: {
    fontWeight: '600',
  },
});
