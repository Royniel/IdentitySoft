import { createContext, useContext, useState, useEffect, type ReactNode } from "react";
import api from "../api/axios";
import type { AuthResponse, AuthUser, Role } from "../types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  updateRoles: (roles: Role[]) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  // On app load, restore session from stored tokens
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const username = localStorage.getItem("username");
    const roles: Role[] = JSON.parse(localStorage.getItem("roles") || "[]");
    if (token && username) {
      setUser({ username, roles });
    }
    setLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    const res = await api.post<AuthResponse>("/auth/login", { username, password });
    persistSession(res.data);
  };

  const register = async (username: string, email: string, password: string) => {
    const res = await api.post<AuthResponse>("/auth/register", { username, email, password });
    persistSession(res.data);
  };

  const persistSession = (data: AuthResponse) => {
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);
    localStorage.setItem("username", data.username);
    localStorage.setItem("roles", JSON.stringify(data.roles || []));
    setUser({ username: data.username, roles: data.roles || [] });
  };

  // Reflects a role change (e.g. removing your own admin access) without a full re-login
  const updateRoles = (roles: Role[]) => {
    localStorage.setItem("roles", JSON.stringify(roles || []));
    setUser((prev) => (prev ? { ...prev, roles: roles || [] } : prev));
  };

  const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("username");
    localStorage.removeItem("roles");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, updateRoles }}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook so components can call useAuth() instead of useContext(AuthContext)
export function useAuth(): AuthContextValue {
  // Assumes AuthProvider always wraps consumers (true everywhere in this app today);
  // the assertion only affects the type seen at compile time, not runtime behavior.
  return useContext(AuthContext) as AuthContextValue;
}
