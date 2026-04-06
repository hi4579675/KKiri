import client from './client';
import { ApiResponse, PostResponse, PresignedUrlResponse, FeedApiResponse } from '../types/api';

export const postApi = {
  async getPresignedUrl(
    groupId: number,
    fileName: string,
    contentType: string,
  ): Promise<PresignedUrlResponse> {
    const res = await client.post<ApiResponse<PresignedUrlResponse>>(
      '/api/posts/presigned-url',
      { groupId, fileName, contentType },
    );
    return res.data.data;
  },

  async uploadToR2(presignedUrl: string, fileUri: string, contentType: string): Promise<void> {
    const response = await fetch(fileUri);
    const blob = await response.blob();

    await fetch(presignedUrl, {
      method: 'PUT',
      headers: { 'Content-Type': contentType },
      body: blob,
    });
  },

  async createPost(data: {
    groupId: number;
    imageKey: string;
    caption?: string;
  }): Promise<PostResponse> {
    const res = await client.post<ApiResponse<PostResponse>>('/api/posts', data);
    return res.data.data;
  },

  async deletePost(postId: number): Promise<void> {
    await client.delete(`/api/posts/${postId}`);
  },

  async getFeed(groupId: number): Promise<FeedApiResponse> {
    const res = await client.get<ApiResponse<FeedApiResponse>>(
      `/api/groups/${groupId}/feed?size=200`,
    );
    return res.data.data;
  },
};
