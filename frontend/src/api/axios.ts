import axios, { type InternalAxiosRequestConfig } from "axios";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface RefreshResponse {
  accessToken: string;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// Central HTTP client — every API call goes through this
const api = axios.create({
  baseURL: API_BASE_URL,
});

// Attach the access token to every outgoing request, if we have one
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the backend says 401 (token expired/invalid), try to refresh once
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as RetryableRequestConfig;

    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const res = await axios.post<RefreshResponse>(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });
          localStorage.setItem("accessToken", res.data.accessToken);
          original.headers.Authorization = `Bearer ${res.data.accessToken}`;
          return api(original); // retry the original request with the new token
        } catch {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          window.location.href = "/login"; // refresh failed → back to login
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
