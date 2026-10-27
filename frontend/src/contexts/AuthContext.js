import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const response = await authAPI.currentUser();
    setUser(response.data);
    return response.data;
  }, []);

  useEffect(() => {
    let active = true;
    const handleUnauthorized = () => setUser(null);
    window.addEventListener('salesnav360:unauthorized', handleUnauthorized);
    authAPI.currentUser()
      .then((response) => {
        if (active) setUser(response.data);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      window.removeEventListener('salesnav360:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = useCallback(async (credentials) => {
    await authAPI.login(credentials);
    return refreshUser();
  }, [refreshUser]);

  const logout = useCallback(async () => {
    try {
      await authAPI.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
