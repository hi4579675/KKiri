import React from 'react';
import { ScrollView, TouchableOpacity, View, Text } from 'react-native';
import { ContributorResponse } from '../../types/api';
import { Avatar } from '../common/Avatar';

interface Props {
  contributors: ContributorResponse[];
  onPressMember: (contributor: ContributorResponse) => void;
}

export function MemberAvatarRow({ contributors, onPressMember }: Props) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ paddingHorizontal: 16, paddingTop: 4, paddingBottom: 12, gap: 16 }}
    >
      {contributors.map((c) => (
        <TouchableOpacity
          key={c.userId}
          onPress={() => onPressMember(c)}
          activeOpacity={0.7}
          style={{ alignItems: 'center', gap: 6 }}
        >
          <View
            style={{
              padding: c.hasPostedToday ? 2 : 0,
              borderRadius: 999,
              borderWidth: c.hasPostedToday ? 2 : 0,
              borderColor: c.hasPostedToday ? c.avatarColor : 'transparent',
            }}
          >
            <Avatar
              emoji={c.avatarEmoji}
              color={c.avatarColor}
              size={48}
              borderWidth={c.hasPostedToday ? 0 : 2}
            />
          </View>
          <Text style={{ color: '#FFFFFF', fontSize: 11 }} numberOfLines={1}>
            {c.nickname}
          </Text>
        </TouchableOpacity>
      ))}
    </ScrollView>
  );
}
