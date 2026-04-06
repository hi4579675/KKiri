import { useState } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { postApi } from '../api/post';
import { PostResponse } from '../types/api';

interface UploadOptions {
  groupId: number;
  caption?: string;
}

export function useImageUpload() {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pickAndUpload = async (options: UploadOptions): Promise<PostResponse | null> => {
    setError(null);

    // 1. 이미지 선택
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.8,
      allowsEditing: true,
      aspect: [1, 1],
    });

    if (result.canceled || !result.assets[0]) return null;

    const asset = result.assets[0];
    const fileName = asset.fileName ?? `photo_${Date.now()}.jpg`;
    const contentType = asset.mimeType ?? 'image/jpeg';

    setIsUploading(true);
    try {
      // 2. Presigned URL 획득
      console.log('[Upload] step1: presigned-url', { groupId: options.groupId, fileName, contentType });
      const { presignedUrl, imageKey } = await postApi.getPresignedUrl(options.groupId, fileName, contentType);
      console.log('[Upload] step1 ok, imageKey:', imageKey);

      // 3. R2에 직접 업로드 (axios 사용 금지 — 서명 깨짐)
      console.log('[Upload] step2: R2 upload');
      await postApi.uploadToR2(presignedUrl, asset.uri, contentType);
      console.log('[Upload] step2 ok');

      // 4. 백엔드에 포스트 생성
      console.log('[Upload] step3: createPost');
      const post = await postApi.createPost({
        groupId: options.groupId,
        imageKey,
        caption: options.caption,
      });
      console.log('[Upload] step3 ok');

      return post;
    } catch (e: any) {
      console.error('[Upload] error:', e?.response?.status, JSON.stringify(e?.response?.data));
      setError('업로드에 실패했습니다.');
      return null;
    } finally {
      setIsUploading(false);
    }
  };

  return { pickAndUpload, isUploading, error };
}
