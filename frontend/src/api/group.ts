import client from './client';
import {
  ApiResponse,
  GroupResponse,
  InviteCodeResponse,
  JoinGroupResponse,
  GroupMember,
} from '../types/api';

export const groupApi = {
  async getMyGroups(): Promise<GroupResponse[]> {
    const res = await client.get<ApiResponse<GroupResponse[]>>('/api/groups/my');
    return res.data.data;
  },

  async createGroup(name: string): Promise<GroupResponse> {
    const res = await client.post<ApiResponse<GroupResponse>>('/api/groups', { name });
    return res.data.data;
  },

  async getInviteCode(groupId: number): Promise<InviteCodeResponse> {
    const res = await client.get<ApiResponse<InviteCodeResponse>>(
      `/api/groups/${groupId}/invite-code`,
    );
    return res.data.data;
  },

  async renewInviteCode(groupId: number): Promise<InviteCodeResponse> {
    const res = await client.post<ApiResponse<InviteCodeResponse>>(
      `/api/groups/${groupId}/invite-code/renew`,
    );
    return res.data.data;
  },

  async joinGroup(inviteCode: string): Promise<JoinGroupResponse> {
    const res = await client.post<ApiResponse<JoinGroupResponse>>('/api/groups/join', {
      inviteCode,
    });
    return res.data.data;
  },

  async getMembers(groupId: number): Promise<GroupMember[]> {
    const res = await client.get<ApiResponse<GroupMember[]>>(
      `/api/groups/${groupId}/members`,
    );
    return res.data.data;
  },

  async kickMember(groupId: number, targetUserId: number): Promise<void> {
    await client.delete(`/api/groups/${groupId}/members/${targetUserId}`);
  },

  async leaveGroup(groupId: number): Promise<void> {
    await client.delete(`/api/groups/${groupId}/members/me`);
  },

  async transferOwner(groupId: number, targetUserId: number): Promise<void> {
    await client.patch(`/api/groups/${groupId}/owner`, { targetUserId });
  },
};
