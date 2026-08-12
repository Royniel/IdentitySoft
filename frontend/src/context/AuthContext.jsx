import { createContext, useContext, useState, useEffect } from "react";
import api from "../api/axios";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // On app load, restore session from stored tokens
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const username = localStorage.getItem("username");
    const roles = JSON.parse(localStorage.getItem("roles") || "[]");
    if (token && username) {
      setUser({ username, roles });
    }
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    const res = await api.post("/auth/login", { username, password });
    persistSession(res.data);
  };

  const register = async (username, email, password) => {
    const res = await api.post("/auth/register", { username, email, password });
    persistSession(res.data);
  };

  const persistSession = (data) => {
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);
    localStorage.setItem("username", data.username);
    localStorage.setItem("roles", JSON.stringify(data.roles || []));
    setUser({ username: data.username, roles: data.roles || [] });
  };

  // Reflects a role change (e.g. removing your own admin access) without a full re-login
  const updateRoles = (roles) => {
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
export function useAuth() {
  return useContext(AuthContext);
}