import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Switch,
  Alert,
  ScrollView,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAuthStore } from '../../../src/store/authStore';
import { useGroupStore } from '../../../src/store/groupStore';
import { useFeedStore } from '../../../src/store/feedStore';
import { userApi } from '../../../src/api/user';
import { useNicknameCheck } from '../../../src/hooks/useNicknameCheck';
import { Avatar } from '../../../src/components/common/Avatar';
import { AVATAR_EMOJIS } from '../../../src/constants/avatarEmojis';
import { AVATAR_COLORS } from '../../../src/constants/avatarColors';

export default function SettingsScreen() {
  const { logout, userProfile, setUserProfile } = useAuthStore();
  const clearGroups = useGroupStore((s) => s.clear);
  const clearFeed = useFeedStore((s) => s.clear);

  const [nickname, setNickname] = useState('');
  const [selectedEmoji, setSelectedEmoji] = useState(AVATAR_EMOJIS[0]);
  const [selectedColor, setSelectedColor] = useState(AVATAR_COLORS[0]);
  const [pushEnabled, setPushEnabled] = useState(true);
  const [saving, setSaving] = useState(false);

  // Whether nickname has been changed from the original
  const [originalNickname, setOriginalNickname] = useState('');
  const nicknameChanged = nickname.trim() !== originalNickname;

  const { available, checking } = useNicknameCheck(nicknameChanged ? nickname : '');

  // Can save if: nickname is non-empty AND (unchanged OR available)
  const canSave =
    nickname.trim().length > 0 &&
    !saving &&
    (!nicknameChanged || available === true);

  useEffect(() => {
    if (userProfile) {
      setNickname(userProfile.nickname);
      setOriginalNickname(userProfile.nickname);
      setSelectedEmoji(userProfile.avatarEmoji);
      setSelectedColor(userProfile.avatarColor);
    } else {
      userApi.getMe().then((p) => {
        setUserProfile(p);
        setNickname(p.nickname);
        setOriginalNickname(p.nickname);
        setSelectedEmoji(p.avatarEmoji);
        setSelectedColor(p.avatarColor);
      }).catch(() => {});
    }
  }, []);

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    try {
      await userApi.updateProfile({
        nickname: nickname.trim(),
        avatarEmoji: selectedEmoji,
        avatarColor: selectedColor,
      });
      setUserProfile({ nickname: nickname.trim(), avatarEmoji: selectedEmoji, avatarColor: selectedColor });
      router.back();
    } catch {
      Alert.alert('오류', '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handlePushToggle = async (v: boolean) => {
    setPushEnabled(v);
    try {
      await userApi.updatePushEnabled(v);
    } catch {
      setPushEnabled(!v);
    }
  };

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃 하시겠어요?', [
      { text: '취소', style: 'cancel' },
      {
        text: '로그아웃',
        style: 'destructive',
        onPress: async () => {
          clearGroups();
          clearFeed();
          await logout();
          router.replace('/(auth)/login');
        },
      },
    ]);
  };

  const nicknameStatusText = () => {
    if (!nicknameChanged || !nickname.trim()) return '';
    if (checking) return '확인 중...';
    if (available === true) return '사용 가능해요';
    if (available === false) return '이미 사용 중이에요';
    return '';
  };

  const nicknameStatusColor = () => {
    if (available === true) return '#34C759';
    if (available === false) return '#FF3B30';
    return '#8E8E93';
  };

  return (
    <ScrollView style={styles.scrollView} contentContainerStyle={{ paddingBottom: 40 }}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="chevron-back" size={24} color="#FFFFFF" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>프로필 설정</Text>
        <TouchableOpacity
          onPress={handleSave}
          disabled={!canSave}
          style={styles.saveButton}
        >
          {saving ? (
            <ActivityIndicator color="#FFFFFF" size="small" />
          ) : (
            <Text style={[styles.saveText, { color: canSave ? '#FFFFFF' : '#3A3A3C' }]}>저장</Text>
          )}
        </TouchableOpacity>
      </View>

      {/* 아바타 미리보기 */}
      <View style={styles.avatarPreviewContainer}>
        <Avatar emoji={selectedEmoji} color={selectedColor} size={80} borderWidth={3} />
      </View>

      {/* 닉네임 */}
      <Text style={styles.sectionLabel}>닉네임</Text>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder="10자 이내로 입력"
          placeholderTextColor="#8E8E93"
          value={nickname}
          onChangeText={setNickname}
          maxLength={10}
          autoCapitalize="none"
        />
      </View>
      {nicknameChanged && (
        <Text style={[styles.nicknameStatus, { color: nicknameStatusColor() }]}>
          {nicknameStatusText()}
        </Text>
      )}

      {/* 이모지 선택 */}
      <Text style={[styles.sectionLabel, { marginTop: 24 }]}>이모지</Text>
      <View style={styles.gridRow}>
        {AVATAR_EMOJIS.map((emoji) => (
          <TouchableOpacity
            key={emoji}
            onPress={() => setSelectedEmoji(emoji)}
            style={[
              styles.emojiButton,
              {
                backgroundColor: selectedEmoji === emoji ? '#3A3A3C' : '#1C1C1E',
                borderWidth: selectedEmoji === emoji ? 2 : 0,
                borderColor: selectedColor,
              },
            ]}
          >
            <Text style={{ fontSize: 24 }}>{emoji}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* 컬러 선택 */}
      <Text style={styles.sectionLabel}>테두리 색상</Text>
      <View style={styles.gridRow}>
        {AVATAR_COLORS.map((color) => (
          <TouchableOpacity
            key={color}
            onPress={() => setSelectedColor(color)}
            style={[
              styles.colorButton,
              {
                backgroundColor: color,
                borderWidth: selectedColor === color ? 3 : 0,
                borderColor: '#FFFFFF',
              },
            ]}
          />
        ))}
      </View>

      {/* 알림 설정 */}
      <View style={styles.section}>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>푸시 알림</Text>
          <Switch
            value={pushEnabled}
            onValueChange={handlePushToggle}
            trackColor={{ false: '#3A3A3C', true: '#34C759' }}
            thumbColor="#FFFFFF"
          />
        </View>
      </View>

      {/* 로그아웃 */}
      <View style={styles.section}>
        <TouchableOpacity style={styles.row} onPress={handleLogout}>
          <Text style={styles.logoutText}>로그아웃</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
    backgroundColor: '#000000',
    paddingHorizontal: 16,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 56,
    paddingBottom: 16,
  },
  backButton: {
    width: 40,
    alignItems: 'flex-start',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '600',
  },
  saveButton: {
    width: 40,
    alignItems: 'flex-end',
  },
  saveText: {
    fontSize: 16,
    fontWeight: '600',
  },
  avatarPreviewContainer: {
    alignItems: 'center',
    marginVertical: 24,
  },
  sectionLabel: {
    color: '#8E8E93',
    fontSize: 12,
    letterSpacing: 2,
    marginBottom: 8,
  },
  inputContainer: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  input: {
    color: '#FFFFFF',
    fontSize: 16,
  },
  nicknameStatus: {
    fontSize: 12,
    marginTop: 4,
  },
  gridRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 24,
  },
  emojiButton: {
    width: 48,
    height: 48,
    borderRadius: 9999,
    alignItems: 'center',
    justifyContent: 'center',
  },
  colorButton: {
    width: 40,
    height: 40,
    borderRadius: 9999,
  },
  section: {
    backgroundColor: '#1C1C1E',
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: 12,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  rowLabel: {
    color: '#FFFFFF',
    fontSize: 16,
  },
  logoutText: {
    color: '#FF3B30',
    fontWeight: '600',
    fontSize: 16,
  },
});
