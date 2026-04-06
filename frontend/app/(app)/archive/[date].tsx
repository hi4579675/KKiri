import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

export default function ArchiveDayScreen() {
  const { date } = useLocalSearchParams<{ date: string }>();

  return (
    <View style={styles.container}>
      <Text style={styles.dateText}>{date}</Text>
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
  dateText: {
    color: '#8E8E93',
  },
});
