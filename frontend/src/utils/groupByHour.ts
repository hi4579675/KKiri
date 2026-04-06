import { HourBucket, PostResponse } from '../types/api';

export function groupByHour(posts: PostResponse[]): HourBucket[] {
  const map = new Map<number, PostResponse[]>();

  for (const post of posts) {
    const bucket = map.get(post.hourBucket) ?? [];
    bucket.push(post);
    map.set(post.hourBucket, bucket);
  }

  return Array.from(map.entries())
    .map(([hour, posts]) => ({ hour, posts }))
    .sort((a, b) => b.hour - a.hour); // 최신 시간 먼저
}

export function formatHourBucket(hour: number): string {
  return `${String(hour).padStart(2, '0')}:00`;
}
