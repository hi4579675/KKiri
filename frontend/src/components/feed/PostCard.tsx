import React, { useState } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  Dimensions,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { PostResponse } from '../../types/api';
import { Avatar } from '../common/Avatar';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { postApi } from '../../api/post';
import { useFeedStore } from '../../store/feedStore';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CARD_MARGIN = 16;
const CARD_WIDTH = SCREEN_WIDTH - CARD_MARGIN * 2;
const CARD_IMG_HEIGHT = Math.round(CARD_WIDTH * 0.56); // 16:9 비율

interface Props {
  post: PostResponse;
  currentUserId?: number;
}

export function PostCard({ post, currentUserId }: Props) {
  const [liked, setLiked] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const removePost = useFeedStore((s) => s.removePost);

  const isOwner = currentUserId !== undefined && post.userId === currentUserId;

  const handleDelete = async () => {
    try {
      await postApi.deletePost(post.postId);
      removePost(post.postId);
    } catch {
      Alert.alert('오류', '삭제에 실패했습니다.');
    }
    setShowDeleteDialog(false);
  };

  const formattedTime = () => {
    const date = new Date(post.createdAt + 'Z'); // UTC → 로컬 시간으로 변환
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    return `${h}:${m}`;
  };

  return (
    <>
      <TouchableOpacity
        activeOpacity={0.95}
        onLongPress={isOwner ? () => setShowDeleteDialog(true) : undefined}
        style={{ marginHorizontal: CARD_MARGIN, marginBottom: 8 }}
      >
        <View style={{ width: CARD_WIDTH, borderRadius: 16, overflow: 'hidden', backgroundColor: '#1C1C1E' }}>
          {/* 멤버 */}
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8 }}>
            <Avatar emoji={post.avatarEmoji} color={post.avatarColor} size={28} borderWidth={2} />
            <Text style={{ color: '#FFFFFF', fontSize: 13, fontWeight: '500' }}>{post.nickname}</Text>
          </View>

          {/* 이미지 */}
          <View style={{ position: 'relative' }}>
            <Image
              source={{ uri: post.imageUrl }}
              style={{ width: CARD_WIDTH, height: CARD_IMG_HEIGHT, resizeMode: 'cover' }}
            />

            {/* 하단 오버레이 */}
            <View
              style={{
                position: 'absolute', bottom: 0, left: 0, right: 0,
                padding: 16, paddingTop: 48,
                backgroundColor: 'rgba(0,0,0,0.4)',
              }}
            >
              <Text style={{ color: '#FFFFFF', fontSize: 28, fontWeight: '700', lineHeight: 34 }}>
                {formattedTime()}
              </Text>
              {post.caption && (
                <Text style={{ color: '#FFFFFF', fontSize: 14, marginTop: 2, opacity: 0.9 }}>
                  {post.caption}
                </Text>
              )}
            </View>

            {/* 액션 버튼 */}
            <View style={{ position: 'absolute', bottom: 16, right: 16, flexDirection: 'row', gap: 8 }}>
              <TouchableOpacity
                onPress={() => setLiked(!liked)}
                style={{
                  width: 40, height: 40, borderRadius: 20,
                  backgroundColor: liked ? '#FF3B30' : 'rgba(60,60,60,0.8)',
                  alignItems: 'center', justifyContent: 'center',
                }}
              >
                <Ionicons name={liked ? 'heart' : 'heart-outline'} size={20} color="#FFFFFF" />
              </TouchableOpacity>
              <TouchableOpacity
                style={{
                  width: 40, height: 40, borderRadius: 20,
                  backgroundColor: 'rgba(60,60,60,0.8)',
                  alignItems: 'center', justifyContent: 'center',
                }}
              >
                <Ionicons name="chatbubble-outline" size={18} color="#FFFFFF" />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </TouchableOpacity>

      <ConfirmDialog
        visible={showDeleteDialog}
        title="포스트 삭제"
        message="이 포스트를 삭제할까요?"
        confirmLabel="삭제"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteDialog(false)}
      />
    </>
  );
}
