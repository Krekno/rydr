import api from '@/services/api';
import { tokenStorage } from '@/services/tokenStorage';
import { AuthResponse } from '@/types/auth';
import { createContext, useContext, useEffect, useState } from 'react';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  signIn: (accessToken: string, refreshToken: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Check whether a token already exists when the app opens
  useEffect(() => {
    async function checkAuth() {
      try {
        const refreshtoken = await tokenStorage.getRefreshToken();
        const response = await api.post<AuthResponse>('/api/auth/refresh', { refreshtoken });
        if (response.status == 200) setIsAuthenticated(true);
      } finally {
        setIsLoading(false);
      }
    }

    checkAuth();
  }, []);

  async function signIn(refreshToken: string, accessToken: string) {
    await tokenStorage.setRefreshToken(refreshToken);
    await tokenStorage.setAccessToken(accessToken);
    setIsAuthenticated(true);
  }

  async function signOut() {
    await tokenStorage.removeRefreshToken();
    await tokenStorage.removeAccessToken();
    setIsAuthenticated(false);
  }

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, isLoading, signIn, signOut }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}