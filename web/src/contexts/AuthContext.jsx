import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../db/api';

const AUTH_USER_KEY = 'auth_user';
const AUTH_TOKEN_KEY = 'auth_token';
const AUTH_REFRESH_TOKEN_KEY = 'auth_refresh_token';
const AUTH_TOKEN_TYPE_KEY = 'auth_token_type';

const AuthContext = createContext(null);

const readStoredUser = () => {
  const rawUser = localStorage.getItem(AUTH_USER_KEY);
  if (!rawUser) {
    return null;
  }

  try {
    return JSON.parse(rawUser);
  } catch {
    localStorage.removeItem(AUTH_USER_KEY);
    return null;
  }
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => readStoredUser());
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY));
  const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem(AUTH_REFRESH_TOKEN_KEY));
  const [tokenType, setTokenType] = useState(() => localStorage.getItem(AUTH_TOKEN_TYPE_KEY) || 'Bearer');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    localStorage.setItem(AUTH_TOKEN_KEY, accessToken);
  }, [accessToken]);

  useEffect(() => {
    if (!user) {
      localStorage.removeItem(AUTH_USER_KEY);
      return;
    }

    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  }, [user]);

  const clearSession = () => {
    setUser(null);
    setAccessToken(null);
    setRefreshToken(null);
    setTokenType('Bearer');
    localStorage.removeItem(AUTH_USER_KEY);
    api.session.clearPersistedTokens();
  };

  const applyTokenBundle = (tokenData) => {
    setAccessToken(tokenData?.accessToken ?? null);
    setRefreshToken(tokenData?.refreshToken ?? null);
    setTokenType(tokenData?.tokenType ?? 'Bearer');
    api.session.persistTokens(tokenData);
  };

  const login = async (email, password) => {
    setIsLoading(true);

    try {
      const tokenData = await api.auth.login({ email, password });

      if (!tokenData?.accessToken) {
        throw new Error('O backend não retornou accessToken.');
      }

      applyTokenBundle(tokenData);

      try {
        const currentUser = await api.users.me();
        setUser(currentUser || { username: email, roles: [] });
      } catch {
        setUser({ username: email, roles: [] });
      }

      return tokenData;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (registrationPayload) => {
    setIsLoading(true);

    try {
      const createdUser = await api.users.register(registrationPayload);
      await login(registrationPayload.email, registrationPayload.password);
      return createdUser;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      if (accessToken) {
        await api.auth.logout();
      }
    } catch {
      // Mesmo com falha de rede/logout no servidor, limpamos sessão local.
    } finally {
      clearSession();
    }
  };

  const value = useMemo(
    () => ({
      user,
      accessToken,
      isLoggedIn: Boolean(accessToken),
      isLoading,
      login,
      register,
      logout,
      clearSession,
      refreshToken,
      tokenType,
    }),
    [user, accessToken, refreshToken, tokenType, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth deve ser utilizado dentro de AuthProvider.');
  }

  return context;
};