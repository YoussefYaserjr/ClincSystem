import axios, { type InternalAxiosRequestConfig } from 'axios';
import { getSession, updateSession } from '../auth';
import type { AuthResponse } from '../types';

const http = axios.create({ baseURL: '/api' });

http.interceptors.request.use((config) => {
  const token = getSession()?.token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshPromise: Promise<AuthResponse> | null = null;

function refreshSession(): Promise<AuthResponse> {
  if (!refreshPromise) {
    const refreshToken = getSession()?.refreshToken;
    refreshPromise = axios
      .post<AuthResponse>('/api/auth/refresh', { refreshToken })
      .then((res) => {
        updateSession({
          userId: res.data.userId,
          role: res.data.role,
          token: res.data.token,
          refreshToken: res.data.refreshToken,
        });
        return res.data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function forceLogin() {
  updateSession(null);
  if (window.location.hash !== '#/login') {
    window.location.hash = '#/login';
  }
}

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const status = error.response?.status;
    const config = error.config as InternalAxiosRequestConfig & { _retried?: boolean };
    if (status !== 401 || config?.url === '/auth/refresh' || config?._retried) {
      return Promise.reject(error);
    }
    if (!getSession()?.refreshToken) {
      forceLogin();
      return Promise.reject(error);
    }
    try {
      await refreshSession();
      config._retried = true;
      return http.request(config);
    } catch {
      forceLogin();
      return Promise.reject(error);
    }
  }
);

export default http;
