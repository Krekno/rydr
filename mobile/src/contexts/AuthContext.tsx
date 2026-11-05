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
        const refreshToken = await tokenStorage.getRefreshToken();

        if (!refreshToken) {
          setIsAuthenticated(false);
          return;
        }

        const response = await api.post<AuthResponse>(
          '/api/auth/refresh',
          { refreshToken }
        );

        if (response.status === 200) {
          await tokenStorage.setAccessToken(response.data.accessToken);
          await tokenStorage.setRefreshToken(response.data.refreshToken);
          setIsAuthenticated(true);
        }
      } catch (error) {
        console.log('Auth initialization failed:', error);
        await tokenStorage.removeAccessToken();
        await tokenStorage.removeRefreshToken();
        setIsAuthenticated(false);
      } finally {
        setIsLoading(false);
      }
    }

    checkAuth();
  }, []);

  async function signIn(accessToken: string, refreshToken: string) {
    await tokenStorage.setAccessToken(accessToken);
    await tokenStorage.setRefreshToken(refreshToken);
    setIsAuthenticated(true);
  }

  async function signOut() {
    try {
      await api.post('/api/auth/logout');
    } catch (e) {
      console.log('Logout API error:', e);
    } finally {
      await tokenStorage.removeRefreshToken();
      await tokenStorage.removeAccessToken();
      setIsAuthenticated(false);
    }
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