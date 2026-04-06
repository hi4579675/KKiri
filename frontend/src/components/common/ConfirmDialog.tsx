import React from 'react';
import { Modal, View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface Props {
  visible: boolean;
  title: string;
  message?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = '확인',
  cancelLabel = '취소',
  destructive = false,
  onConfirm,
  onCancel,
}: Props) {
  return (
    <Modal transparent visible={visible} animationType="fade" onRequestClose={onCancel}>
      <View style={styles.overlay}>
        <View style={styles.card}>
          <Text style={styles.titleText}>{title}</Text>
          {message && (
            <Text style={styles.messageText}>{message}</Text>
          )}
          {!message && <View style={styles.spacer} />}
          <View style={styles.buttonRow}>
            <TouchableOpacity
              style={styles.cancelButton}
              onPress={onCancel}
            >
              <Text style={styles.cancelButtonText}>{cancelLabel}</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.confirmButton, { backgroundColor: destructive ? '#FF3B30' : '#FFFFFF' }]}
              onPress={onConfirm}
            >
              <Text
                style={[styles.confirmButtonText, { color: destructive ? '#FFFFFF' : '#000000' }]}
              >
                {confirmLabel}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
  },
  card: {
    width: 288,
    borderRadius: 16,
    backgroundColor: '#1C1C1E',
    padding: 24,
  },
  titleText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 8,
  },
  messageText: {
    color: '#8E8E93',
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 24,
  },
  spacer: {
    marginBottom: 16,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 12,
  },
  cancelButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 12,
    backgroundColor: '#2C2C2E',
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#FFFFFF',
    fontWeight: '500',
  },
  confirmButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  confirmButtonText: {
    fontWeight: '500',
  },
});
