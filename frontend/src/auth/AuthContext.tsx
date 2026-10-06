import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { apiFetch } from '../api/client';
import type { AuthUser } from './types';

type AuthContextValue = {
  currentUser: AuthUser | null;
  isLoading: boolean;
  isInitializing: boolean;
  initializationError: string | null;
  retrySession: () => void;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<AuthUser>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [sessionCheckAttempt, setSessionCheckAttempt] = useState(0);

  useEffect(() => {
    let mounted = true;

    async function restoreSession() {
      setIsInitializing(true);
      setInitializationError(null);
      try {
        const user = await apiFetch<AuthUser>('/api/auth/me');
        if (mounted) setCurrentUser(user);
      } catch (cause) {
        if (!mounted) return;
        if (cause instanceof Error && 'status' in cause && cause.status === 401) {
          setCurrentUser(null);
        } else {
          setInitializationError('Can’t reach the server. Please try again.');
        }
      } finally {
        if (mounted) setIsInitializing(false);
      }
    }

    void restoreSession();
    return () => {
      mounted = false;
    };
  }, [sessionCheckAttempt]);

  function retrySession() {
    setSessionCheckAttempt((attempt) => attempt + 1);
  }

  async function login(email: string, password: string) {
    setIsLoading(true);
    try {
      const user = await apiFetch<AuthUser>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      setCurrentUser(user);
    } finally {
      setIsLoading(false);
    }
  }

  async function register(email: string, password: string) {
    return apiFetch<AuthUser>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async function logout() {
    try {
      await apiFetch<void>('/api/auth/logout', { method: 'POST' });
    } finally {
      setCurrentUser(null);
    }
  }

  const value = useMemo(
    () => ({
      currentUser,
      isLoading,
      isInitializing,
      initializationError,
      retrySession,
      login,
      register,
      logout,
    }),
    [currentUser, isLoading, isInitializing, initializationError],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}