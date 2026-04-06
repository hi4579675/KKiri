import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Image,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { postApi } from '../../../src/api/post';
import { useGroupStore } from '../../../src/store/groupStore';
import { useFeedStore } from '../../../src/store/feedStore';

export default function UploadScreen() {
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [imageKey, setImageKey] = useState<string | null>(null);
  const [caption, setCaption] = useState('');
  const [uploading, setUploading] = useState(false);
  const [posting, setPosting] = useState(false);

  const activeGroupId = useGroupStore((s) => s.activeGroupId);
  const prependPost = useFeedStore((s) => s.prependPost);

  // 화면 진입할 때마다 초기화
  useFocusEffect(
    useCallback(() => {
      setImageUri(null);
      setImageKey(null);
      setCaption('');
      setUploading(false);
      setPosting(false);
    }, []),
  );

  const handlePickImage = async () => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      Alert.alert('권한 필요', '사진 접근 권한이 필요해요.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.85,
      allowsEditing: false,
    });

    if (result.canceled || !result.assets[0]) return;

    const asset = result.assets[0];
    const fileName = asset.fileName ?? `photo_${Date.now()}.jpg`;
    const contentType = asset.mimeType ?? 'image/jpeg';

    setImageUri(asset.uri);
    setImageKey(null);
    setUploading(true);

    try {
      if (!activeGroupId) throw new Error('그룹이 선택되지 않았습니다.');
      const { presignedUrl, imageKey: key } = await postApi.getPresignedUrl(activeGroupId, fileName, contentType);

      const r2Res = await fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': contentType },
        body: await (await fetch(asset.uri)).blob(),
      });

      if (!r2Res.ok) throw new Error(`R2 upload failed: ${r2Res.status}`);

      setImageKey(key);
    } catch (e) {
      console.error('Upload error:', e);
      Alert.alert('업로드 실패', 'R2 서버에 사진을 올리지 못했어요. 다시 시도해주세요.');
      setImageUri(null);
    } finally {
      setUploading(false);
    }
  };

  const handlePost = async () => {
    if (!activeGroupId || !imageKey) return;
    setPosting(true);
    try {
      const post = await postApi.createPost({
        groupId: activeGroupId,
        imageKey,
        caption: caption.trim() || undefined,
      });
      prependPost(post);
      router.replace('/(app)/feed');
    } catch {
      Alert.alert('오류', '포스트 등록에 실패했습니다.');
    } finally {
      setPosting(false);
    }
  };

  const canPost = !!imageKey && !uploading && !posting;

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="close" size={26} color="#8E8E93" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>새 포스트</Text>
        <TouchableOpacity onPress={handlePost} disabled={!canPost}>
          <Text
            style={[styles.headerAction, { color: canPost ? '#FFFFFF' : '#3A3A3C' }]}
          >
            올리기
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 40 }}
        keyboardShouldPersistTaps="handled"
      >
        {/* 사진 선택 영역 */}
        <TouchableOpacity
          onPress={handlePickImage}
          activeOpacity={0.8}
          style={{
            width: '100%',
            aspectRatio: 1,
            borderRadius: 16,
            overflow: 'hidden',
            backgroundColor: '#1C1C1E',
            marginBottom: 16,
          }}
        >
          {imageUri ? (
            <>
              <Image
                source={{ uri: imageUri }}
                style={{ width: '100%', height: '100%' }}
                resizeMode="cover"
              />
              {/* 업로드 중 오버레이 */}
              {uploading && (
                <View
                  style={{
                    position: 'absolute',
                    inset: 0,
                    backgroundColor: 'rgba(0,0,0,0.55)',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <ActivityIndicator color="#FFFFFF" size="large" />
                  <Text style={styles.uploadingText}>업로드 중...</Text>
                </View>
              )}
              {/* 업로드 완료 뱃지 */}
              {imageKey && !uploading && (
                <View
                  style={{
                    position: 'absolute',
                    bottom: 12,
                    right: 12,
                    backgroundColor: 'rgba(0,0,0,0.6)',
                    borderRadius: 20,
                    paddingHorizontal: 10,
                    paddingVertical: 5,
                    flexDirection: 'row',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  <Ionicons name="checkmark-circle" size={14} color="#34C759" />
                  <Text style={{ color: '#FFFFFF', fontSize: 12 }}>사진 선택됨</Text>
                </View>
              )}
              {/* 사진 교체 버튼 */}
              {!uploading && (
                <View
                  style={{
                    position: 'absolute',
                    top: 12,
                    right: 12,
                    backgroundColor: 'rgba(0,0,0,0.6)',
                    borderRadius: 20,
                    padding: 6,
                  }}
                >
                  <Ionicons name="camera" size={18} color="#FFFFFF" />
                </View>
              )}
            </>
          ) : (
            <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 }}>
              <Ionicons name="image-outline" size={64} color="#3A3A3C" />
              <Text style={styles.pickImageText}>탭해서 사진 선택</Text>
            </View>
          )}
        </TouchableOpacity>

        {/* 글 입력 */}
        <View style={styles.captionContainer}>
          <TextInput
            style={styles.captionInput}
            placeholder="한 줄 메모 (선택)"
            placeholderTextColor="#8E8E93"
            value={caption}
            onChangeText={(t) => setCaption(t.slice(0, 30))}
            maxLength={30}
            returnKeyType="done"
          />
          <Text style={styles.captionCount}>{caption.length}/30</Text>
        </View>

        {/* 하단 올리기 버튼 (보조) */}
        <TouchableOpacity
          style={[styles.postButton, { backgroundColor: canPost ? '#FFFFFF' : '#2C2C2E' }]}
          onPress={handlePost}
          disabled={!canPost}
        >
          {posting ? (
            <ActivityIndicator color="#000000" />
          ) : (
            <Text
              style={[styles.postButtonText, { color: canPost ? '#000000' : '#3A3A3C' }]}
            >
              {!imageUri ? '사진을 먼저 선택해주세요' : uploading ? '업로드 중...' : '올리기'}
            </Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
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
    fontWeight: '600',
    fontSize: 16,
  },
  headerAction: {
    fontSize: 16,
    fontWeight: '600',
  },
  uploadingText: {
    color: '#FFFFFF',
    fontSize: 14,
    marginTop: 12,
  },
  pickImageText: {
    color: '#8E8E93',
    fontSize: 14,
  },
  captionContainer: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  captionInput: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 16,
  },
  captionCount: {
    color: '#8E8E93',
    fontSize: 12,
    marginLeft: 8,
  },
  postButton: {
    marginTop: 24,
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  postButtonText: {
    fontWeight: '600',
    fontSize: 16,
  },
});
