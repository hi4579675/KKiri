import { create } from 'zustand';
import { GroupResponse, GroupMember } from '../types/api';

interface GroupState {
  groups: GroupResponse[];
  activeGroupId: number | null;
  members: Record<number, GroupMember[]>;

  setGroups: (groups: GroupResponse[]) => void;
  addGroup: (group: GroupResponse) => void;
  removeGroup: (groupId: number) => void;
  updateGroup: (groupId: number, updates: Partial<GroupResponse>) => void;
  setActiveGroup: (groupId: number) => void;
  setMembers: (groupId: number, members: GroupMember[]) => void;
  clear: () => void;
}

export const useGroupStore = create<GroupState>((set) => ({
  groups: [],
  activeGroupId: null,
  members: {},

  setGroups: (groups) =>
    set({ groups, activeGroupId: groups.length > 0 ? groups[0].groupId : null }),

  addGroup: (group) =>
    set((s) => ({ groups: [...s.groups, group], activeGroupId: group.groupId })),

  removeGroup: (groupId) =>
    set((s) => {
      const groups = s.groups.filter((g) => g.groupId !== groupId);
      return {
        groups,
        activeGroupId:
          s.activeGroupId === groupId ? (groups[0]?.groupId ?? null) : s.activeGroupId,
      };
    }),

  updateGroup: (groupId, updates) =>
    set((s) => ({
      groups: s.groups.map((g) => (g.groupId === groupId ? { ...g, ...updates } : g)),
    })),

  setActiveGroup: (groupId) => set({ activeGroupId: groupId }),

  setMembers: (groupId, members) =>
    set((s) => ({ members: { ...s.members, [groupId]: members } })),

  clear: () => set({ groups: [], activeGroupId: null, members: {} }),
}));
