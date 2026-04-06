import React from 'react';
import { View, Text } from 'react-native';

interface Props {
  emoji: string;
  color: string;
  size?: number;
  borderWidth?: number;
}

export function Avatar({ emoji, color, size = 44, borderWidth = 2.5 }: Props) {
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        borderWidth,
        borderColor: color,
        backgroundColor: '#1C1C1E',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Text style={{ fontSize: size * 0.45 }}>{emoji}</Text>
    </View>
  );
}
