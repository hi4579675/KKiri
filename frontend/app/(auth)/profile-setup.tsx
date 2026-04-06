import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
  StyleSheet,
} from 'react-native';
import { router } from 'expo-router';
import { userApi } from '../../src/api/user';
import { useAuthStore } from '../../src/store/authStore';
import { useNicknameCheck } from '../../src/hooks/useNicknameCheck';
import { AVATAR_EMOJIS } from '../../src/constants/avatarEmojis';
import { AVATAR_COLORS } from '../../src/constants/avatarColors';

export default function ProfileSetupScreen() {
  const [nickname, setNickname] = useState('');
  const [selectedEmoji, setSelectedEmoji] = useState(AVATAR_EMOJIS[0]);
  const [selectedColor, setSelectedColor] = useState(AVATAR_COLORS[0]);
  const [loading, setLoading] = useState(false);
  const { available, checking } = useNicknameCheck(nickname);
  const setProfileCompleted = useAuthStore((s) => s.setProfileCompleted);

  const canSubmit = nickname.trim().length > 0 && available === true && !loading;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setLoading(true);
    try {
      await userApi.completeProfile({
        nickname: nickname.trim(),
        avatarEmoji: selectedEmoji,
        avatarColor: selectedColor,
      });
      setProfileCompleted(true);
      router.replace('/(app)/feed');
    } catch {
      Alert.alert('오류', '프로필 설정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const nicknameStatusText = () => {
    if (!nickname.trim()) return '';
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
    <ScrollView
      style={styles.scrollView}
      contentContainerStyle={{ paddingTop: 80, paddingBottom: 40 }}
    >
      <Text style={styles.title}>프로필 설정</Text>
      <Text style={styles.subtitle}>친구들에게 보여질 내 모습을 설정해요</Text>

      {/* 아바타 미리보기 */}
      <View style={styles.avatarPreviewContainer}>
        <View
          style={[styles.avatarPreview, { borderColor: selectedColor, backgroundColor: '#1C1C1E' }]}
        >
          <Text style={{ fontSize: 44 }}>{selectedEmoji}</Text>
        </View>
      </View>

      {/* 닉네임 입력 */}
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
      <Text style={[styles.nicknameStatus, { color: nicknameStatusColor() }]}>
        {nicknameStatusText()}
      </Text>

      {/* 이모지 선택 */}
      <Text style={styles.sectionLabel}>이모지</Text>
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

      {/* 완료 버튼 */}
      <TouchableOpacity
        style={[styles.submitButton, { backgroundColor: canSubmit ? '#FFFFFF' : '#3A3A3C' }]}
        onPress={handleSubmit}
        disabled={!canSubmit}
      >
        {loading ? (
          <ActivityIndicator color="#000000" />
        ) : (
          <Text style={[styles.submitButtonText, { color: canSubmit ? '#000000' : '#8E8E93' }]}>
            완료
          </Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
    backgroundColor: '#000000',
    paddingHorizontal: 24,
  },
  title: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '700',
    marginBottom: 4,
  },
  subtitle: {
    color: '#8E8E93',
    fontSize: 14,
    marginBottom: 40,
  },
  avatarPreviewContainer: {
    alignItems: 'center',
    marginBottom: 40,
  },
  avatarPreview: {
    width: 96,
    height: 96,
    borderRadius: 9999,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
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
    marginBottom: 4,
  },
  input: {
    color: '#FFFFFF',
    fontSize: 16,
  },
  nicknameStatus: {
    fontSize: 12,
    marginBottom: 32,
  },
  gridRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 32,
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
  submitButton: {
    width: '100%',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  submitButtonText: {
    fontWeight: '600',
    fontSize: 16,
  },
});
