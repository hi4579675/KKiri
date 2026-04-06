export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  profileCompleted: boolean;
}

export interface PostResponse {
  postId: number;
  userId: number;
  nickname: string;
  avatarEmoji: string;
  avatarColor: string;
  imageUrl: string;
  caption: string | null;
  hourBucket: number;
  createdAt: string;
}

export interface GroupResponse {
  groupId: number;
  name: string;
  inviteCode: string;
  inviteCodeExpiredAt: string;
  maxMembers: number;
}

export interface InviteCodeResponse {
  groupId: number;
  inviteCode: string;
  inviteCodeExpiredAt: string;
}

export interface JoinGroupResponse {
  groupId: number;
  name: string;
  memberCount: number;
}

export interface PresignedUrlResponse {
  presignedUrl: string;
  imageKey: string;
  imageUrl: string;
}

export interface GroupMember {
  userId: number;
  nickname: string;
  avatarEmoji: string;
  avatarColor: string;
  role: 'OWNER' | 'MEMBER';
  joinedAt: string;
}

export interface HourBucket {
  hour: number;
  posts: PostResponse[];
}

export interface ContributorResponse {
  userId: number;
  nickname: string;
  avatarEmoji: string;
  avatarColor: string;
  hasPostedToday: boolean;
}

export interface UserProfile {
  userId: number;
  nickname: string;
  avatarEmoji: string;
  avatarColor: string;
}

export interface FeedApiResponse {
  contributors: ContributorResponse[];
  buckets: { hourBucket: number; posts: PostResponse[] }[];
  nextCursor: number | null;
}
