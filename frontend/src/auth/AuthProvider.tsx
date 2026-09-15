import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, authenticate, refreshSession, setAccessToken, setSessionLostHandler } from '../api/client';
import type { CurrentUser } from '../api/types';

interface AuthState {
  user: CurrentUser | null;
  /** True until the initial session recovery finishes, so routes do not flash sign-in. */
  loading: boolean;
  signIn: (identifier: string, password: string) => Promise<void>;
  signUp: (input: SignUpInput) => Promise<void>;
  signInWithGoogle: (idToken: string) => Promise<void>;
  signOut: () => Promise<void>;
  /** Re-reads the account after it changes, such as a handle rename. */
  refreshCurrentUser: () => Promise<void>;
}

export interface SignUpInput {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();

  const loadCurrentUser = useCallback(async () => {
    setUser(await api.get<CurrentUser>('/api/v1/me'));
  }, []);

  // On a page load there is no access token in memory, but the refresh cookie may still be
  // valid. Trying it once is what makes a reload keep the user signed in.
  useEffect(() => {
    let cancelled = false;

    void (async () => {
      const renewed = await refreshSession();
      if (cancelled) {
        return;
      }
      if (renewed) {
        try {
          await loadCurrentUser();
        } catch {
          setUser(null);
        }
      }
      setLoading(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [loadCurrentUser]);

  // The client calls this when a refresh fails, which is the one case the UI cannot see itself.
  useEffect(() => {
    setSessionLostHandler(() => {
      setUser(null);
      queryClient.clear();
    });
    return () => setSessionLostHandler(null);
  }, [queryClient]);

  const signIn = useCallback(
    async (identifier: string, password: string) => {
      await authenticate('/api/v1/auth/login', { identifier, password });
      await loadCurrentUser();
    },
    [loadCurrentUser],
  );

  const signUp = useCallback(
    async (input: SignUpInput) => {
      await api.post('/api/v1/auth/register', input);
      // Registration does not sign the user in, so follow it with a login.
      await authenticate('/api/v1/auth/login', {
        identifier: input.username,
        password: input.password,
      });
      await loadCurrentUser();
    },
    [loadCurrentUser],
  );

  const signInWithGoogle = useCallback(
    async (idToken: string) => {
      // Identical to a password sign-in from here on: the response carries our own access token
      // and sets our own refresh cookie.
      await authenticate('/api/v1/auth/google', { idToken });
      await loadCurrentUser();
    },
    [loadCurrentUser],
  );

  const signOut = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout');
    } finally {
      // Clear locally even if the call failed: the user asked to be signed out.
      setAccessToken(null);
      setUser(null);
      queryClient.clear();
    }
  }, [queryClient]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      signIn,
      signUp,
      signInWithGoogle,
      signOut,
      refreshCurrentUser: loadCurrentUser,
    }),
    [user, loading, signIn, signUp, signInWithGoogle, signOut, loadCurrentUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
