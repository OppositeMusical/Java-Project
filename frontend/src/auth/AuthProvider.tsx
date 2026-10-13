import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ApiError, authApi, setUnauthorizedHandler } from '../api/client';
import type { LoginRequest, RegisterRequest, User } from '../api/types';

interface AuthState {
  /** `undefined` while the initial session check is in flight, `null` when signed out. */
  user: User | null | undefined;
  signIn: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null | undefined>(undefined);

  useEffect(() => {
    // Restores the session after a page reload (and gives the browser its CSRF cookie).
    authApi
      .me()
      .then(setUser)
      .catch((error) => {
        if (!(error instanceof ApiError && error.status === 401)) console.error(error);
        setUser(null);
      });
    setUnauthorizedHandler(() => setUser(null));
  }, []);

  const signIn = useCallback(async (request: LoginRequest) => setUser(await authApi.login(request)), []);
  const register = useCallback(async (request: RegisterRequest) => setUser(await authApi.register(request)), []);
  const signOut = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ user, signIn, register, signOut }), [user, signIn, register, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
