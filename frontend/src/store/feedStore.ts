import { create } from 'zustand';
import { FeedApiResponse, HourBucket, PostResponse } from '../types/api';
import { groupByHour } from '../utils/groupByHour';

interface FeedState {
  buckets: HourBucket[];
  contributors: FeedApiResponse['contributors'];
  isLoading: boolean;
  wsConnected: boolean;

  setPosts: (posts: PostResponse[]) => void;
  setFeedResponse: (feed: FeedApiResponse) => void;
  prependPost: (post: PostResponse) => void;
  removePost: (postId: number) => void;
  setLoading: (v: boolean) => void;
  setWsConnected: (v: boolean) => void;
  clear: () => void;
}

export const useFeedStore = create<FeedState>((set, get) => ({
  buckets: [],
  contributors: [],
  isLoading: false,
  wsConnected: false,

  setPosts: (posts) => set({ buckets: groupByHour(posts) }),

  setFeedResponse: (feed) => set({
    buckets: feed.buckets.map((b) => ({ hour: b.hourBucket, posts: b.posts })),
    contributors: feed.contributors,
  }),

  prependPost: (post) => {
    const buckets = get().buckets;
    const existing = buckets.find((b) => b.hour === post.hourBucket);
    if (existing) {
      set({
        buckets: buckets.map((b) =>
          b.hour === post.hourBucket ? { ...b, posts: [post, ...b.posts] } : b,
        ),
      });
    } else {
      const newBuckets = [{ hour: post.hourBucket, posts: [post] }, ...buckets].sort(
        (a, b) => b.hour - a.hour,
      );
      set({ buckets: newBuckets });
    }
  },

  removePost: (postId) =>
    set((s) => ({
      buckets: s.buckets
        .map((b) => ({ ...b, posts: b.posts.filter((p) => p.postId !== postId) }))
        .filter((b) => b.posts.length > 0),
    })),

  setLoading: (v) => set({ isLoading: v }),
  setWsConnected: (v) => set({ wsConnected: v }),
  clear: () => set({ buckets: [], contributors: [], isLoading: false }),
}));
