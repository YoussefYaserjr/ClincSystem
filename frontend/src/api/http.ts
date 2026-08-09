import axios from 'axios';

const http = axios.create({ baseURL: '/api' });

http.interceptors.request.use((config) => {
  const raw = localStorage.getItem('clinic.user');
  if (raw) {
    try {
      const token = JSON.parse(raw).token as string;
      if (token) config.headers.Authorization = `Bearer ${token}`;
    } catch {
      // ignore malformed stored session
    }
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('clinic.user');
      if (window.location.hash !== '#/login') {
        window.location.hash = '#/login';
      }
    }
    return Promise.reject(error);
  }
);

export default http;
