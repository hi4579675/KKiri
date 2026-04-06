import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

export default function ArchiveScreen() {
  return (
    <View style={styles.container}>
      <Text style={{ fontSize: 40 }}>📁</Text>
      <Text style={styles.title}>아카이브</Text>
      <Text style={styles.subtitle}>지난 하루들을 돌아봐요</Text>
      <Text style={styles.wip}>(준비 중)</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
    marginTop: 16,
  },
  subtitle: {
    color: '#8E8E93',
    fontSize: 14,
    marginTop: 8,
  },
  wip: {
    color: '#8E8E93',
    fontSize: 12,
    marginTop: 32,
  },
});
