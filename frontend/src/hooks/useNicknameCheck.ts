import { useState, useEffect } from 'react';
import { userApi } from '../api/user';

export function useNicknameCheck(nickname: string) {
  const [available, setAvailable] = useState<boolean | null>(null);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    if (!nickname.trim()) {
      setAvailable(null);
      return;
    }

    setChecking(true);
    setAvailable(null);

    const timer = setTimeout(async () => {
      try {
        const result = await userApi.checkNickname(nickname);
        setAvailable(result.available);
      } catch {
        setAvailable(null);
      } finally {
        setChecking(false);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [nickname]);

  return { available, checking };
}
