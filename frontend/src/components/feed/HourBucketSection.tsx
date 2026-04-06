import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { HourBucket, PostResponse } from '../../types/api';
import { PostCard } from './PostCard';
import { formatHourBucket } from '../../utils/groupByHour';

interface Props {
  bucket: HourBucket;
  currentUserId?: number;
}

export function HourBucketSection({ bucket, currentUserId }: Props) {
  const uniqueAuthors = new Set(bucket.posts.map((p) => p.author?.userId)).size;

  return (
    <View style={styles.section}>
      <View style={styles.headerRow}>
        <Text style={styles.hourText}>{formatHourBucket(bucket.hour)}</Text>
        {uniqueAuthors > 0 && (
          <Text style={styles.authorCount}>{uniqueAuthors}명</Text>
        )}
      </View>
      {bucket.posts.map((post) => (
        <PostCard key={post.postId} post={post} currentUserId={currentUserId} />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  section: {
    marginBottom: 16,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 12,
  },
  hourText: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '700',
  },
  authorCount: {
    color: '#8E8E93',
    fontSize: 14,
    marginLeft: 8,
  },
});
