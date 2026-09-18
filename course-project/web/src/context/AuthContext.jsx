import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../lib/api.js';
import { clearApiCache } from '../lib/dataCache.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!localStorage.getItem('feynman_token')) {
      setLoading(false);
      return;
    }

    apiRequest('/auth/me')
      .then(({ user: currentUser }) => setUser(currentUser))
      .catch(() => localStorage.removeItem('feynman_token'))
      .finally(() => setLoading(false));
  }, []);

  const signIn = async (payload, mode) => {
    const result = await apiRequest(`/auth/${mode}`, { method: 'POST', body: JSON.stringify(payload) });
    localStorage.setItem('feynman_token', result.token);
    clearApiCache();
    setUser(result.user);
  };

  const signOut = () => {
    localStorage.removeItem('feynman_token');
    clearApiCache();
    setUser(null);
  };

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

